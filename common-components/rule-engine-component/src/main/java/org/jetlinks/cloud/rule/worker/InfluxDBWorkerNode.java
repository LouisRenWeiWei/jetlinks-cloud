package org.jetlinks.cloud.rule.worker;

import com.alibaba.fastjson.JSON;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.utils.StringUtils;
import org.hswebframework.utils.time.DateFormatter;
import org.hswebframework.web.ExpressionUtils;
import org.hswebframework.web.bean.ToString;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Point;
import org.jetlinks.rule.engine.api.RuleData;
import org.jetlinks.rule.engine.api.executor.ExecutionContext;
import org.jetlinks.rule.engine.api.model.NodeType;
import org.jetlinks.rule.engine.executor.AbstractExecutableRuleNodeFactoryStrategy;
import org.jetlinks.rule.engine.executor.supports.RuleNodeConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author zhouhao
 * @since 1.0.0
 */
@Component
@ConditionalOnClass(InfluxDB.class)
@Slf4j
public class InfluxDBWorkerNode extends AbstractExecutableRuleNodeFactoryStrategy<InfluxDBWorkerNode.Config> {

    @Override
    public Config newConfig() {
        return new Config();
    }

    @Override
    public String getSupportType() {
        return "influx-write";
    }

    @Override
    public Function<RuleData, CompletionStage<Object>> createExecutor(ExecutionContext context, Config config) {
        config.init();

        InfluxDB influxDB = config.newInfluxDB();

        AtomicReference<FluxSink<RuleData>> sinkReference = new AtomicReference<>();

        Disposable disposable = Flux.create(sinkReference::set)
                .bufferTimeout(config.getBufferSize(), Duration.ofSeconds(2))
                .subscribe(list -> {
                    try {
                        influxDB.write(config.parsePoint(context, list));
                        if (log.isInfoEnabled()) {
                            log.info("保存规则数据到InfluxDB成功,数量:{}", list.size());
                        }
                    } catch (Throwable e) {
                        context.onError(RuleData.create(list), e);
                    }
                });

        context.onStop(() -> {
            sinkReference.get().complete();
            disposable.dispose();
            influxDB.close();
        });

        return data -> {
            sinkReference.get().next(data);

            return CompletableFuture.completedFuture(data);
        };
    }

    @Getter
    @Setter
    public static class PointConvert {

        private String measurement;

        private String timeField = "now";


        private Map<String, String> fieldMapping = new HashMap<>();

        public void validate() {
            Assert.hasText(measurement, "measurement");
        }

        public Point convert(ExecutionContext context, RuleData ruleData) {
            Map<String, Object> expressionContext = new HashMap<>();
            expressionContext.put("ruleData", ruleData);
            Object value = ruleData.getData();
            if (value instanceof Map) {
                expressionContext.putAll(((Map) value));
            }
            long time = System.currentTimeMillis();
            if (!"now".equals(timeField)) {
                Object timeVal = expressionContext.get(timeField);
                if (timeVal instanceof Long) {
                    time = ((Long) timeVal);
                } else if (timeVal instanceof Date) {
                    time = ((Date) timeVal).getTime();
                }
            }
            Point.Builder builder = Point.measurement(measurement)
                    .time(time, TimeUnit.MILLISECONDS)
                    .addField("dataId", ruleData.getId());
            for (Map.Entry<String, String> entry : fieldMapping.entrySet()) {
                String val = entry.getValue();
                try {
                    val = ExpressionUtils.analytical(entry.getValue(), expressionContext, "spel");
                } catch (Exception e) {
                    context.logger().error("解析表达式[{}]=[{}]失败", entry.getKey(), val, e);
                }
                if (StringUtils.isDouble(val)) {
                    builder.addField(entry.getKey(), StringUtils.toDouble(val));
                } else if (StringUtils.isNumber(val)) {
                    builder.addField(entry.getKey(), StringUtils.toLong(val));
                } else if (StringUtils.isBoolean(val)) {
                    builder.addField(entry.getKey(), Boolean.valueOf(val));
                } else if (DateFormatter.isSupport(val)) {
                    builder.addField(entry.getKey(), DateFormatter.fromString(val).getTime());
                } else {
                    builder.addField(entry.getKey(), val);
                }
            }
            return builder.build();
        }
    }

    @Getter
    @Setter
    public static class Config implements RuleNodeConfig {

        private String url;

        private String username;

        @ToString.Ignore
        private String password;

        private String database;

        private String convertsJson;

        private List<PointConvert> converts = new ArrayList<>();

        private NodeType nodeType;

        private int bufferSize = 200;

        public InfluxDB newInfluxDB() {
            Assert.hasText(url, "url");
            Assert.hasText(username, "username");
            Assert.hasText(password, "password");
            Assert.hasText(database, "database");

            InfluxDB influxDB = InfluxDBFactory.connect(url);
            influxDB.setDatabase(database);
            return influxDB;
        }

        public void init() {
            if (converts.isEmpty() && org.springframework.util.StringUtils.hasText(convertsJson)) {
                converts = JSON.parseArray(convertsJson, PointConvert.class);
                converts.forEach(PointConvert::validate);
            }
        }

        public BatchPoints parsePoint(ExecutionContext context, Collection<RuleData> data) {
            BatchPoints.Builder points = BatchPoints.builder();

            List<Point> pointList = data.parallelStream()
                    .flatMap(ruleData -> converts.stream().map(convert -> convert.convert(context, ruleData)))
                    .collect(Collectors.toList());
            points.points(pointList);
            return points.build();
        }

        public BatchPoints parsePoint(ExecutionContext context, RuleData data) {
            BatchPoints.Builder points = BatchPoints.builder();
            converts.stream()
                    .map(convert -> convert.convert(context, data))
                    .forEach(points::point);
            return points.build();
        }

    }
}

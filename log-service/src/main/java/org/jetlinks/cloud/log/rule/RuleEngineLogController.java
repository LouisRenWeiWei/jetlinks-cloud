package org.jetlinks.cloud.log.rule;

import io.searchbox.client.JestClient;
import io.searchbox.core.Search;
import org.hswebframework.web.controller.message.ResponseMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rule-engine")
public class RuleEngineLogController {

    @Autowired
    private JestClient jestClient;

//    @GetMapping("/{instanceId}/{nodeId}/logs")
//    public ResponseMessage<Object> getLog(@PathVariable  String instanceId,
//                                          @PathVariable String nodeId){
//
//        Search.Builder builder = new Search.Builder();
//
//    }
}

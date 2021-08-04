package com.ly.community53.config;

import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.RestClients;

/**
 * @author malaka
 * @create 2021-01-14 9:43
 */
@Configuration   // 使用配置类的方式来注入bean  ElasticsearchRestTemplate
public class EsConf {
    @Value("${elasticSearch.url}")
    private String edUrl;

    //localhost:9200 写在配置文件中就可以了
    @Bean
    RestHighLevelClient client() {
        ClientConfiguration clientConfiguration = ClientConfiguration.builder()
                .connectedTo(edUrl)//elasticsearch地址
                .build();

        return RestClients.create(clientConfiguration).rest();
    }
}


package com.ly.community53.dao.elasticsearch;

import com.ly.community53.entity.DiscussPost;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DiscussPostRepository extends ElasticsearchRepository<DiscussPost, Integer> {
//<DiscussPost, Integer>泛型，分别是es的存储类型、主键
}

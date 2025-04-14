package Douban;

import redis.clients.jedis.*;
import redis.clients.jedis.resps.Tuple;

import java.util.*;


/**
 * 1. 核心数据结构
 * 该类使用的是 Redis 的 ZSet（有序集合）：
 *
 * key 是 "douban:rank"
 *
 * value 是每个 movieName（电影名）
 *
 * score 是 double 类型的评分（比如 9.5）
 *
 * Redis 的 ZSet 支持：
 *
 * 自动按 score 排序（升序 / 降序）
 *
 * 支持 range 查询、排名、插入、删除等操作
 *
 * 内部是通过跳表 + 哈希表实现的
 * */
public class DoubanRankService {
    // Redis 的连接池，复用连接，避免频繁建立连接造成性能浪费
    private static final JedisPool pool = new JedisPool("localhost", 6379);

    /**
     * 添加或更新电影评分
     * - 使用 Redis 的 Sorted Set（ZSet）数据结构。
     * - 如果 movieName 已存在，会自动更新其 score。
     * - Redis 内部会重新排序以维护 score 顺序。
     * @param movieName 电影名称（作为成员）
     * @param score     电影评分（作为分值）
     */
    public void addScore(String movieName, double score) {
        try (Jedis jedis = pool.getResource()) {
            jedis.zadd("douban:rank", score, movieName);
        }
    }

    /**
     * 获取评分排名前 K 的电影
     * - zrevrangeWithScores 从高到低返回前 K 项及其评分
     * 返回结果是 Set<Tuple>，每个 Tuple 有 .getElement() 和 .getScore()。
     * @param K 要获取的前 K 名
     * @return List<Tuple>，Tuple 包含 member 和 score
     */
    public List<Tuple> getTopK(int K) {
        try (Jedis jedis = pool.getResource()) {
            return new ArrayList<>(jedis.zrevrangeWithScores("douban:rank", 0, K - 1));
        }
    }

    /**
     * 获取某个电影的当前排名（越小表示排名越靠前）
     * - 使用 zrevrank，从高分到低分排序
     * - 排名从 0 开始
     * @param movieName 电影名称
     * @return 当前排名（null 表示该电影未在榜中）
     */
    public Long getRank(String movieName) {
        try (Jedis jedis = pool.getResource()) {
            return jedis.zrevrank("douban:rank", movieName);
        }
    }
}

package com.hmdp.utils;

import cn.hutool.core.lang.UUID;
import com.hmdp.service.IFollowService;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

public class SimpleRedisLock implements ILock{

    private String name;
    private StringRedisTemplate stringRedisTemplate;
    private static final String KEY_PREFIX = "lock:";
    private static final String ID_PREFIX = UUID.randomUUID().toString(true)+"-";

    public SimpleRedisLock(String name, StringRedisTemplate stringRedisTemplate) {
        this.name = name;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean tryLock(long timeoutSec) {

        String id = ID_PREFIX+Thread.currentThread().getId();

        Boolean aBoolean = stringRedisTemplate.
                opsForValue().
                setIfAbsent(KEY_PREFIX + name, id + "", timeoutSec, TimeUnit.SECONDS);

        return Boolean.TRUE.equals(aBoolean);
    }

    @Override
    public void unlock() {

        String threadId = ID_PREFIX+Thread.currentThread().getId();

        String id = stringRedisTemplate.opsForValue().get(KEY_PREFIX + name);

        if (threadId.equals(id)){
            stringRedisTemplate.delete(KEY_PREFIX+name);
        }

    }

}

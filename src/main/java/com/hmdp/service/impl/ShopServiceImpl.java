package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.annotation.Transient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 *
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryById(Long id) {

        Shop shop = queryWithPassThrough(id);
        if (null == shop){
            return Result.fail("店铺不存在！");
        }
        return Result.ok(shop);
    }

    //
    public Shop queryWithPassThrough(Long id){

        String key = CACHE_SHOP_KEY+id;
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        if (!StrUtil.isBlank(shopJson)){
            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
            return shop;
        }

        if (shopJson !=null){
            return null;
        }

//获取互斥锁
        Shop shop = null;
        try {
            boolean isLocked = tryLock(LOCK_SHOP_KEY + id);
            if (!isLocked){
                Thread.sleep(100);
                queryWithPassThrough(id);
            }

            shop = getById(id);
            if (shop == null){
                //缓存穿透返回空
                stringRedisTemplate.opsForValue().set(key,"",CACHE_NULL_TTL, TimeUnit.MINUTES);
                return null;
            }

            stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(shop),CACHE_SHOP_TTL, TimeUnit.MINUTES);

        }catch (Exception e){
            e.printStackTrace();
        }finally {
            releaseLock(LOCK_SHOP_KEY + id);
        }

        return shop;

    }

    @Override
    @Transient
    public Result update(Shop shop) {
        Long id = shop.getId();
        if (id == null){
            return Result.fail("店铺ID不能为空!");
        }
        String key = CACHE_SHOP_KEY+id;

        updateById(shop);

        stringRedisTemplate.delete(key);

        return Result.ok();
    }


    private boolean tryLock(String key){
        Boolean absent = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(absent);
    }

    private void releaseLock(String key){
        stringRedisTemplate.delete(key);
    }
}

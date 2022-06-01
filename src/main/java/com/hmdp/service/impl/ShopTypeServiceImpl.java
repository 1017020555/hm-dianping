package com.hmdp.service.impl;

import cn.hutool.core.collection.ListUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 *
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryTypeList() {
        List<String> shopType = stringRedisTemplate.opsForList().range("shop:type",0,-1);
        if (shopType.isEmpty()){
            List<ShopType> typeList = query().orderByAsc("sort").list();
            stringRedisTemplate.opsForList().leftPushAll("shop:type",typeList.toString());
        }

        return Result.ok();
    }


}

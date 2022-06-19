package com.hmdp.service.impl;

import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.SimpleRedisLock;
import com.hmdp.utils.UserHolder;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 *
 * @since 2021-12-22
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    @Lazy
    private ISeckillVoucherService iSeckillVoucherService;

    @Resource
    private RedisIdWorker redisIdWorker;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedissonClient redissonClient;

    @Override
    public Result seckillVoucher(Long voucherId) {
        //秒杀开始-秒杀结束-库存是否充足-扣库存
        SeckillVoucher seckillVoucher = iSeckillVoucherService.getById(voucherId);

        LocalDateTime beginTime = seckillVoucher.getBeginTime();
        if (beginTime.isAfter(LocalDateTime.now())){
            return Result.fail("秒杀未开始！");
        }

        LocalDateTime endTime = seckillVoucher.getEndTime();
        if (endTime.isBefore(LocalDateTime.now())){
            return Result.fail("秒杀已结束！");
        }

        if (seckillVoucher.getStock() < 1){
            return Result.fail("库存不足！");
        }

        //        一人一单
        Long userId = UserHolder.getUser().getId();

//        SimpleRedisLock simpleRedisLock = new SimpleRedisLock("order:" + userId, stringRedisTemplate);

        RLock lock = redissonClient.getLock("lock:order:" + userId);


        boolean isLock = lock.tryLock();

        if (!isLock){
            return Result.fail("不允许重复下单！");
        }

        try {
            IVoucherOrderService iVoucherOrderService = (IVoucherOrderService) AopContext.currentProxy();
            return iVoucherOrderService.createVoucherOrder(voucherId);
        }finally {
            lock.unlock();
        }



    }

    @Transactional
    public Result createVoucherOrder(Long voucherId) {

        Long userId = UserHolder.getUser().getId();

        Integer count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();
        if ( count > 0){
            return Result.fail("该用户已购买过!！");
        }

//       扣库存
        boolean succcess = iSeckillVoucherService.
                update().setSql("stock = stock -1")
                .eq("voucher_id", voucherId).gt("stock",0).update();
        if (!succcess){
            return Result.fail("库存不足!！");
        }

//        创建订单
        VoucherOrder voucherOrder = new VoucherOrder();
        voucherOrder.setId(redisIdWorker.nextId("order"));
        voucherOrder.setUserId(UserHolder.getUser().getId());
        voucherOrder.setVoucherId(voucherId);

        save(voucherOrder);

        return Result.ok(voucherOrder.getId());


    }


}

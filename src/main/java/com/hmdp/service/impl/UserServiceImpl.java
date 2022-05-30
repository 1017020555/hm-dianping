package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexPatterns;
import com.hmdp.utils.RegexUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpSession;

import static com.hmdp.utils.SystemConstants.USER_NICK_NAME_PREFIX;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 *
 * @since 2021-12-22
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {


    @Override
    public Result sendCode(String phone, HttpSession session) {
        if (RegexUtils.isPhoneInvalid(phone)){
            return Result.fail("手机号不符合规则！");
        }

        String numbers = RandomUtil.randomNumbers(6);

        session.setAttribute("code",numbers);


        log.info("发生短信验证码："+numbers);
        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        if (RegexUtils.isPhoneInvalid(loginForm.getPhone())){
            return Result.fail("手机号不符合规则！");
        }
        Object code = session.getAttribute("code");
        String formCode = loginForm.getCode();
        if (code == null || !code.toString().equals(formCode)){
            return Result.fail("验证码错误！");
        }
        User user = query().eq("phone", formCode).one();
        if (user == null){
           user = createUserByPhone(formCode);
        }
        UserDTO userDTO = new UserDTO();
        BeanUtil.copyProperties(user,userDTO);

        session.setAttribute("user",userDTO);

        return Result.ok();
    }

    private User createUserByPhone(String formCode) {
        User user =new User();
        user.setPhone(formCode);
        user.setNickName(USER_NICK_NAME_PREFIX+ RandomUtil.randomString(8));
        save(user);
        return user;
    }


}

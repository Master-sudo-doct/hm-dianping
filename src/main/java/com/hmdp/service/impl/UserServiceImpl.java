package com.hmdp.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.ExceptionContants;
import com.hmdp.utils.RegexUtils;
import com.hmdp.utils.SystemConstants;
import com.hmdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Override
    public Result sendCode(String phone, HttpSession session) {
        //0.校验手机号
        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail(ExceptionContants.INVALID_PHONENUMBER);
        }
        //1.生成短信验证码
        String code = RandomUtil.randomNumbers(6);
        //2.保存验证码到session
        session.setAttribute(SystemConstants.USER_SECURITY_CODE, code);
        //3.向客户端发送验证码
        log.debug("验证码：{}", code);
        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        //0.校验手机号
        if (loginForm.getCode() == null || RegexUtils.isPhoneInvalid(loginForm.getPhone())) {
            return Result.fail(ExceptionContants.INVALID_PHONENUMBER);
        }
        //1.比对验证码
        if (loginForm.getCode() == null || !loginForm.getCode().equals(session.getAttribute(SystemConstants.USER_SECURITY_CODE))) {
            return Result.fail(ExceptionContants.INVALID_CODE);
        }
        //2.查询数据库
        User user = query().eq("phone", loginForm.getPhone()).one();
        //3.用户不存在则为用户注册
        if (user == null) {
            user = createUser(loginForm);
        }
        UserDTO userDTO = new UserDTO();
        BeanUtils.copyProperties(user, userDTO);
        //4.存入session
        session.setAttribute(SystemConstants.USER, user);
        return Result.ok(userDTO);
    }

    @Override
    public Result getCurrentUser() {
        UserDTO user = UserHolder.getUser();
        return Result.ok(user);
    }

    @Override
    public Result logout(HttpSession session) {
        session.removeAttribute(SystemConstants.USER);
        return Result.ok();
    }

    private User createUser(LoginFormDTO loginForm) {
        User user = new User();
        user.setPhone(loginForm.getPhone());
        user.setNickName(SystemConstants.USER_NICK_NAME_PREFIX + RandomUtil.randomString(10));
        save(user);
        return user;
    }
}

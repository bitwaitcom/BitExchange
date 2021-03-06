package com.bitwait.bitrade.aspect;

import com.bitwait.bitrade.constant.SysConstant;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.bitwait.bitrade.entity.Advertise;
import com.bitwait.bitrade.entity.Order;
import com.bitwait.bitrade.entity.transform.AuthMember;
import com.bitwait.bitrade.service.AdvertiseService;
import com.bitwait.bitrade.service.LocaleMessageSourceService;
import com.bitwait.bitrade.service.OrderService;

import javax.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * 修改支付信息前必须下架所有的广告，并且没有正在进行中的订单
 *
 * @author ragan QQ:2098401701 E-mail:bitwait@qq.com
 * @date 2020年03月28日
 */
@Aspect
@Component
@Slf4j
public class UpdatePayAspect {
    @Autowired
    private LocaleMessageSourceService msService;
    @Autowired
    private AdvertiseService advertiseService;
    @Autowired
    private OrderService orderService;

    private ThreadLocal<Long> startTime = new ThreadLocal<>();

    @Pointcut("execution(public * com.bitwait.bitrade.controller.ApproveController.updateAli(..))"+
            "||execution(public * com.bitwait.bitrade.controller.ApproveController.updateBank(..))"+
            "||execution(public * com.bitwait.bitrade.controller.ApproveController.updateWechat(..))")
    public void updatePay() {
    }

    @Before("updatePay()")
    public void doBefore(JoinPoint joinPoint) throws Throwable {
        log.info("❤❤❤❤❤❤❤❤❤❤❤❤❤❤❤❤❤❤❤❤❤❤❤❤❤❤❤❤❤❤❤❤❤❤❤❤❤❤❤❤❤❤❤❤❤❤❤❤❤❤❤❤❤❤❤❤❤❤❤");
        check(joinPoint);
    }

    public void check(JoinPoint joinPoint) throws Exception {
        startTime.set(System.currentTimeMillis());
        // 接收到请求，记录请求内容
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes.getRequest();
        AuthMember authMember = (AuthMember) request.getSession().getAttribute(SysConstant.SESSION_MEMBER);
        List<Order> list1 = orderService.getAllOrdering(authMember.getId());
        if (list1.size()>0){
            throw new IllegalArgumentException(msService.getMessage("HAVE_ORDER_ING"));
        }
        List<Advertise> list = advertiseService.getAllPutOnAdvertis(authMember.getId());
        if (list.size()>0){
            throw new IllegalArgumentException(msService.getMessage("MUST_PUT_OFF_ALL_ADVERTISE"));
        }
    }

    @AfterReturning(pointcut = "updatePay()")
    public void doAfterReturning() throws Throwable {
        log.info("处理耗时：" + (System.currentTimeMillis() - startTime.get()) + "ms");
        log.info("↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑");
        startTime.remove();
    }
}

package com.bitwait.bitrade.controller.finance;

import static com.bitwait.bitrade.constant.BooleanEnum.IS_FALSE;
import static com.bitwait.bitrade.constant.WithdrawStatus.FAIL;
import static com.bitwait.bitrade.constant.WithdrawStatus.SUCCESS;
import static com.bitwait.bitrade.constant.WithdrawStatus.WAITING;
import static org.springframework.util.Assert.isTrue;
import static org.springframework.util.Assert.notNull;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import com.bitwait.bitrade.annotation.AccessLog;
import com.bitwait.bitrade.constant.AdminModule;
import com.bitwait.bitrade.constant.BooleanEnum;
import com.bitwait.bitrade.constant.PageModel;
import com.bitwait.bitrade.constant.SysConstant;
import com.bitwait.bitrade.constant.TransactionType;
import com.bitwait.bitrade.constant.WithdrawStatus;
import com.bitwait.bitrade.controller.common.BaseAdminController;
import com.bitwait.bitrade.entity.Admin;
import com.bitwait.bitrade.entity.Member;
import com.bitwait.bitrade.entity.MemberTransaction;
import com.bitwait.bitrade.entity.MemberWallet;
import com.bitwait.bitrade.entity.QMember;
import com.bitwait.bitrade.entity.QWithdrawRecord;
import com.bitwait.bitrade.entity.WithdrawRecord;
import com.bitwait.bitrade.es.ESUtils;
import com.bitwait.bitrade.model.screen.WithdrawRecordScreen;
import com.bitwait.bitrade.service.LocaleMessageSourceService;
import com.bitwait.bitrade.service.MemberService;
import com.bitwait.bitrade.service.MemberTransactionService;
import com.bitwait.bitrade.service.MemberWalletService;
import com.bitwait.bitrade.service.WithdrawRecordService;
import com.bitwait.bitrade.util.MessageResult;
import com.bitwait.bitrade.vendor.provider.SMSProvider;
import com.bitwait.bitrade.vo.WithdrawRecordVO;
import com.querydsl.core.types.Predicate;
import com.sparkframework.security.Encrypt;

import lombok.extern.slf4j.Slf4j;

/**
 * @author ragan QQ:2098401701 E-mail:bitwait@qq.com
 * @description ??????
 * @date 2019/2/25 11:22
 */
@RestController
@Slf4j
@RequestMapping("/finance/withdraw-record")
public class WithdrawRecordController extends BaseAdminController {
    @Autowired
    private WithdrawRecordService withdrawRecordService;

    @Autowired
    private MemberWalletService memberWalletService;

    @Autowired
    private MemberTransactionService memberTransactionService;

    @Autowired
    private LocaleMessageSourceService messageSource;
    @Autowired
    private ESUtils esUtils;

    @Autowired
    private SMSProvider smsProvider;
    
    @Autowired
    private MemberService memberService;


    @Value("${spark.system.md5.key}")
    private String md5Key;

    @RequiresPermissions("finance:withdraw-record:all")
    @GetMapping("/all")
    @AccessLog(module = AdminModule.FINANCE, operation = "??????????????????WithdrawRecord")
    public MessageResult all() {
        List<WithdrawRecord> withdrawRecordList = withdrawRecordService.findAll();
        if (withdrawRecordList == null || withdrawRecordList.size() < 1) {
            return error(messageSource.getMessage("NO_DATA"));
        }
        return success(withdrawRecordList);
    }

    @RequiresPermissions(value = {"finance:withdraw-record:page-query", "finance:withdraw-record:page-query:success"}, logical = Logical.OR)
    @RequestMapping("/page-query")
    @AccessLog(module = AdminModule.FINANCE, operation = "????????????????????????WithdrawRecord")
    public MessageResult pageQuery(
            PageModel pageModel,
            WithdrawRecordScreen screen) {
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(QWithdrawRecord.withdrawRecord.memberId.eq(QMember.member.id));

        if (screen.getMemberId() != null) {
            predicates.add(QWithdrawRecord.withdrawRecord.memberId.eq(screen.getMemberId()));
        }

        if ( !StringUtils.isEmpty(screen.getMobilePhone())){
            Member member = memberService.findByPhone(screen.getMobilePhone());
            predicates.add(QWithdrawRecord.withdrawRecord.memberId.eq(member.getId()));
        }

        if ( !StringUtils.isEmpty(screen.getOrderSn())){
            predicates.add(QWithdrawRecord.withdrawRecord.transactionNumber.eq(screen.getOrderSn()));
        }

        if (screen.getStatus() != null) {
            predicates.add(QWithdrawRecord.withdrawRecord.status.eq(screen.getStatus()));
        }

        if (screen.getIsAuto() != null) {
            predicates.add(QWithdrawRecord.withdrawRecord.isAuto.eq(screen.getIsAuto()));
        }

        if (!StringUtils.isEmpty(screen.getAddress())) {
            predicates.add(QWithdrawRecord.withdrawRecord.address.eq(screen.getAddress()));
        }

        if (!StringUtils.isEmpty(screen.getUnit())) {
            predicates.add(QWithdrawRecord.withdrawRecord.coin.unit.equalsIgnoreCase(screen.getUnit()));
        }

        if (!StringUtils.isEmpty(screen.getAccount())) {
            predicates.add(QMember.member.username.like("%" + screen.getAccount() + "%")
                    .or(QMember.member.realName.like("%" + screen.getAccount() + "%")));
        }

        Page<WithdrawRecordVO> pageListMapResult = withdrawRecordService.joinFind(predicates, pageModel);
        return success(pageListMapResult);
    }

    @GetMapping("/{id}")
    @RequiresPermissions("finance:withdraw-record:detail")
    @AccessLog(module = AdminModule.FINANCE, operation = "????????????WithdrawRecord ??????")
    public MessageResult detail(@PathVariable("id") Long id) {
        WithdrawRecord withdrawRecord = withdrawRecordService.findOne(id);
        notNull(withdrawRecord, messageSource.getMessage("NO_DATA"));
        return success(withdrawRecord);
    }

    //??????????????????
    @RequiresPermissions("finance:withdraw-record:audit-pass")
    @PatchMapping("/audit-pass")
    @AccessLog(module = AdminModule.FINANCE, operation = "????????????WithdrawRecord??????????????????")
    public MessageResult auditPass(@RequestParam("ids") Long[] ids) {
        withdrawRecordService.audit(ids, WAITING);
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String date = df.format(new Date());
        for(Long id : ids) {
        	WithdrawRecord record = withdrawRecordService.findOne(id);
        	Member member = memberService.findOne(record.getMemberId());
        	if(member != null) {
        		try {
					smsProvider.sendCustomMessage(member.getMobilePhone(), "??????????????????????????????" + date + "?????? " + record.getCoin().getUnit() + "??????????????????" + record.getArrivedAmount() + "???");
				} catch (Exception e) {
					e.printStackTrace();
				}
        	}
        }
        return success(messageSource.getMessage("PASS_THE_AUDIT"));
    }

    //?????????????????????
    @RequiresPermissions("finance:withdraw-record:audit-no-pass")
    @PatchMapping("/audit-no-pass")
    @AccessLog(module = AdminModule.FINANCE, operation = "????????????WithdrawRecord?????????????????????")
    public MessageResult auditNoPass(@RequestParam("ids") Long[] ids) {
        withdrawRecordService.audit(ids, FAIL);
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String date = df.format(new Date());
        for(Long id : ids) {
        	WithdrawRecord record = withdrawRecordService.findOne(id);
        	Member member = memberService.findOne(record.getMemberId());
        	if(member != null) {
        		try {
					smsProvider.sendCustomMessage(member.getMobilePhone(), "???????????????????????????????????????" + date + "?????? " + record.getCoin().getUnit() + "??????????????????" + record.getArrivedAmount() + "???????????????????????????");
				} catch (Exception e) {
					e.printStackTrace();
				}
        	}
        }
        return success(messageSource.getMessage("AUDIT_DOES_NOT_PASS"));
    }

    /**
     * ???????????? ???????????????????????????
     *
     * @param id
     * @param transactionNumber
     * @return
     */
    @RequiresPermissions("finance:withdraw-record:add-transaction-number")
    @PatchMapping("/add-transaction-number")
    @AccessLog(module = AdminModule.FINANCE, operation = "?????????????????????")
    @Transactional(rollbackFor = Exception.class)
    public MessageResult addNumber(
            @RequestParam("id") Long id,
            @RequestParam("transactionNumber") String transactionNumber) {
        WithdrawRecord record = withdrawRecordService.findOne(id);
        Assert.notNull(record, "??????????????????");
        Assert.isTrue(record.getIsAuto() == BooleanEnum.IS_FALSE, "???????????????????????????");
        record.setTransactionNumber(transactionNumber);
        record.setStatus(WithdrawStatus.SUCCESS);
        MemberWallet memberWallet = memberWalletService.findByCoinAndMemberId(record.getCoin(), record.getMemberId());
        Assert.notNull(memberWallet, "member id " + record.getMemberId() + " ??? wallet ??? null");
        memberWallet.setFrozenBalance(memberWallet.getFrozenBalance().subtract(record.getTotalAmount()));
        memberWalletService.save(memberWallet);
        record = withdrawRecordService.save(record);

        MemberTransaction memberTransaction = new MemberTransaction();
        memberTransaction.setMemberId(record.getMemberId());
        memberTransaction.setAddress(record.getAddress());
        memberTransaction.setAmount(record.getTotalAmount());
        memberTransaction.setSymbol(record.getCoin().getUnit());
        memberTransaction.setCreateTime(record.getCreateTime());
        memberTransaction.setType(TransactionType.WITHDRAW);
        memberTransaction.setFee(record.getFee());
        memberTransaction.setRealFee(record.getFee()+"");
        memberTransaction.setDiscountFee("0");
        memberTransaction= memberTransactionService.save(memberTransaction);

        return MessageResult.success(messageSource.getMessage("SUCCESS"), record);
    }

    //????????????
    @RequiresPermissions("finance:withdraw-record:remittance")
    @PatchMapping("/remittance")
    @AccessLog(module = AdminModule.FINANCE, operation = "????????????/????????????")
    @Transactional(rollbackFor = Exception.class)
    public MessageResult remittance(
            @SessionAttribute(SysConstant.SESSION_ADMIN) Admin admin,
            @RequestParam("ids") Long[] ids,
            @RequestParam("transactionNumber") String transactionNumber,
            @RequestParam("password") String password) {
        Assert.notNull(admin, messageSource.getMessage("DATA_EXPIRED_LOGIN_AGAIN"));
        password = Encrypt.MD5(password + md5Key);
        if (!password.equals(admin.getPassword())) {
            return error(messageSource.getMessage("WRONG_PASSWORD"));
        }
        WithdrawRecord withdrawRecord;
        for (Long id : ids) {
            withdrawRecord = withdrawRecordService.findOne(id);
            notNull(withdrawRecord, "id :" + id + messageSource.getMessage("NO_DATA"));
            isTrue(withdrawRecord.getStatus() == WAITING, "??????????????????????????????,????????????!");
            isTrue(withdrawRecord.getIsAuto() == IS_FALSE, "????????????????????????!");
            //??????????????????
            withdrawRecord.setStatus(SUCCESS);
            //????????????
            withdrawRecord.setTransactionNumber(transactionNumber);
            MemberWallet memberWallet = memberWalletService.findByCoinAndMemberId(withdrawRecord.getCoin(), withdrawRecord.getMemberId());
            Assert.notNull(memberWallet, "member id " + withdrawRecord.getMemberId() + " ??? wallet ??? null");
            memberWallet.setFrozenBalance(memberWallet.getFrozenBalance().subtract(withdrawRecord.getTotalAmount()));
            memberWalletService.save(memberWallet);
            withdrawRecordService.save(withdrawRecord);

            MemberTransaction memberTransaction = new MemberTransaction();
            memberTransaction.setMemberId(withdrawRecord.getMemberId());
            memberTransaction.setAddress(withdrawRecord.getAddress());
            memberTransaction.setAmount(withdrawRecord.getTotalAmount());
            memberTransaction.setSymbol(withdrawRecord.getCoin().getUnit());
            memberTransaction.setCreateTime(withdrawRecord.getCreateTime());
            memberTransaction.setType(TransactionType.WITHDRAW);
            memberTransaction.setFee(withdrawRecord.getFee());
            memberTransaction.setRealFee(withdrawRecord.getFee()+"");
            memberTransaction.setDiscountFee("0");
            memberTransaction= memberTransactionService.save(memberTransaction);

        }
        return success();
    }

    /**
     * ???????????????????????????????????????
     * @param pageModel
     * @param screen
     * @param memberId
     * @return
     */
    @RequiresPermissions("finance:withdraw-record:superwithdraw-page-query")
    @PostMapping(value = "/superwithdraw-page-query")
    @AccessLog(module = AdminModule.FINANCE, operation = "???????????????????????????????????????WithdrawRecordController")
    @Transactional(rollbackFor = Exception.class)
    public MessageResult pageQuerySuper(
            PageModel pageModel,
            WithdrawRecordScreen screen,
            Long memberId) {

        Member checkMember = memberService.findOne(memberId);
        if(!checkMember.getSuperPartner().equals("1")) {
            return error("?????????????????????");
        }

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(QWithdrawRecord.withdrawRecord.memberId.eq(QMember.member.id));
        predicates.add(QMember.member.inviterId.eq(checkMember.getId()));

        if (screen.getMemberId() != null) {
            predicates.add(QWithdrawRecord.withdrawRecord.memberId.eq(screen.getMemberId()));
        }

        if ( !StringUtils.isEmpty(screen.getMobilePhone())){
            Member member = memberService.findByPhone(screen.getMobilePhone());
            predicates.add(QWithdrawRecord.withdrawRecord.memberId.eq(member.getId()));
        }

        if ( !StringUtils.isEmpty(screen.getOrderSn())){
            predicates.add(QWithdrawRecord.withdrawRecord.transactionNumber.eq(screen.getOrderSn()));
        }

        if (screen.getStatus() != null) {
            predicates.add(QWithdrawRecord.withdrawRecord.status.eq(screen.getStatus()));
        }

        if (screen.getIsAuto() != null) {
            predicates.add(QWithdrawRecord.withdrawRecord.isAuto.eq(screen.getIsAuto()));
        }

        if (!StringUtils.isEmpty(screen.getAddress())) {
            predicates.add(QWithdrawRecord.withdrawRecord.address.eq(screen.getAddress()));
        }

        if (!StringUtils.isEmpty(screen.getUnit())) {
            predicates.add(QWithdrawRecord.withdrawRecord.coin.unit.equalsIgnoreCase(screen.getUnit()));
        }

        if (!StringUtils.isEmpty(screen.getAccount())) {
            predicates.add(QMember.member.username.like("%" + screen.getAccount() + "%")
                    .or(QMember.member.realName.like("%" + screen.getAccount() + "%")));
        }

        Page<WithdrawRecordVO> pageListMapResult = withdrawRecordService.joinFind(predicates, pageModel);
        return success(pageListMapResult);
    }
}

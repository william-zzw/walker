package walker.application.coordinator.service.impl;

import static walker.application.coordinator.CoordinatorConst.TransactionTxStatus.RECORDED;
import static walker.application.coordinator.CoordinatorConst.TransactionTxStatus.WAITE_COMMIT;

import java.util.List;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import walker.application.coordinator.entity.WalkerTransaction;
import walker.application.coordinator.entity.WalkerTransactionExample;
import walker.application.coordinator.mapper.WalkerTransactionMapper;
import walker.application.coordinator.service.WalkerTransactionService;
import walker.common.util.Utility;
import walker.protocol.message.command.TransactionCommand;

/**
 * @author SONG
 */
@Service
public class WalkerTransactionServiceImpl implements WalkerTransactionService {

    @Resource
    private WalkerTransactionMapper walkerTransactionMapper;

    @Override
    @Transactional(rollbackFor = {Exception.class})
    public int publish(WalkerTransaction transaction) {
        return walkerTransactionMapper.insertSelective(transaction);
    }

    @Override
    @Transactional(rollbackFor = {Exception.class})
    public void postCommit(TransactionCommand.TransactionCommit command) {
        WalkerTransactionExample example = new WalkerTransactionExample();
        example.createCriteria()
                .andAppIdEqualTo(command.getAppId())
                .andMasterGidEqualTo(command.getMasterGid())
                .andBranchGidEqualTo(command.getBranchGid())
                .andTxStatusEqualTo(RECORDED);
        List<WalkerTransaction> transactionList = walkerTransactionMapper.selectByExample(example);
        if (!CollectionUtils.isEmpty(transactionList)) {
            WalkerTransaction transaction = transactionList.get(0);

            WalkerTransaction transactionUpdate = new WalkerTransaction();
            transactionUpdate.setGmtModified(Utility.unix_timestamp());
            transactionUpdate.setTxStatus(WAITE_COMMIT);

            WalkerTransactionExample updateExample = new WalkerTransactionExample();
            updateExample.createCriteria().andIdEqualTo(transaction.getId());
            walkerTransactionMapper.updateByExampleSelective(transactionUpdate, updateExample);

            if (transaction.getIsDeclare()) {
                /**
                 * 控制权在declare 或者 coordinator
                 */
                // 新增notify_task记录
            }
        }
    }

    @Override
    @Transactional(rollbackFor = {Exception.class})
    public void postCancel(TransactionCommand.TransactionBroken toCommand) {

    }
}

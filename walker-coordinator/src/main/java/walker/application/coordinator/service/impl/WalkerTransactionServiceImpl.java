package walker.application.coordinator.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import walker.application.coordinator.entity.WalkerTransaction;
import walker.application.coordinator.entity.WalkerTransactionExample;
import walker.application.coordinator.mapper.WalkerTransactionMapper;
import walker.application.coordinator.service.WalkerTransactionService;
import walker.common.util.Utility;
import walker.protocol.message.command.TransactionCommand;

import javax.annotation.Resource;
import java.util.List;

import static walker.application.coordinator.CoordinatorConst.WalkerTransactionStatus.*;

@Service
public class WalkerTransactionServiceImpl implements WalkerTransactionService {

    @Resource
    private WalkerTransactionMapper walkerTransactionMapper;

    @Override
    @Transactional
    public int add(WalkerTransaction transaction) {
        return walkerTransactionMapper.insertSelective(transaction);
    }

    @Override
    @Transactional
    public void doCommit(TransactionCommand.TransactionCommit command) {
        WalkerTransactionExample example = new WalkerTransactionExample();
        example.createCriteria()
                .andAppIdEqualTo(command.getAppId())
                .andMasterGidEqualTo(command.getMasterGid())
                .andBranchGidEqualTo(command.getBranchGid())
                .andStatusEqualTo(ADDED);
        List<WalkerTransaction> transactionList = walkerTransactionMapper.selectByExample(example);
        if (!CollectionUtils.isEmpty(transactionList)) {
            WalkerTransaction transaction = transactionList.get(0);

            WalkerTransaction transactionUpdate = new WalkerTransaction();
            transactionUpdate.setGmtModified(Utility.getTimestamp());
            transactionUpdate.setStatus(COMMIT);

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
    @Transactional
    public void doBroken(TransactionCommand.TransactionBroken toCommand) {

    }
}

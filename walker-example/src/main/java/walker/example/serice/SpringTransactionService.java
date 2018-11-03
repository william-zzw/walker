package walker.example.serice;

import com.google.common.collect.Maps;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import walker.common.annotation.TccTransaction;
import walker.common.compensate.CompensateMode;
import walker.common.context.WalkerContext;
import walker.common.context.WalkerContextlManager;
import walker.example.model.YourBusinessModel;
import walker.example.rpc.RemoteFeignService;
import walker.protocol.compensate.Participant;

import java.util.Map;

/**
 * needJudge
 * @see walker.core.warpper.TccTransactionResultJudge
 * @see walker.core.warpper.DefaultTccTransactionResultJudge#ok
 *
 * mode
 * CompensateMode.ASYNC  not seriously business mothod , not fail-fast
 * CompensateMode.SYNC  it will seriously business mothod, but it's fail-fast
 *
 */
@Service
public class SpringTransactionService {

    @Autowired
    private RemoteFeignService remoteFeignService;

    @TccTransaction(id = "must be unique id",
                needJudge = false,
                mode = CompensateMode.ASYNC,
                commitMethodName = "diy_commit", commitMethodArgs = {"#yourBusinessModel.gid", "#yourBusinessModel.boolParam"},
                cancelMethodName = "diy_cancel", cancelMethodArgs = {"#yourBusinessModel.doubleParam", "#yourBusinessModel.longParam", "#p0.gid"})
    @Transactional(rollbackFor = {Exception.class})
    public void aroundWithWalkerTransactionManagement(YourBusinessModel yourBusinessModel) {

        /**
         * input your business code
         */
        aroundWithSpringTransactionManagement(yourBusinessModel);
    }

    @Transactional(rollbackFor = {Exception.class})
    public void aroundWithSpringTransactionManagement(YourBusinessModel yourBusinessModel) {

        /**
         * input your business code
         */
        remoteFeignService.pay(yourBusinessModel);

        System.out.println("I will rollback with my brother");

        withWalkerTransactionManagement(yourBusinessModel.getGid(), yourBusinessModel.isBoolParam(), yourBusinessModel.getDoubleParam(), yourBusinessModel.getLongParam());
    }


    @TccTransaction(id = "must be unique id!",
            needJudge = false,
            mode = CompensateMode.ASYNC,
            commitMethodName = "diy_commit", commitMethodArgs = {"#gid", "#b"},
            cancelMethodName = "diy_cancel", cancelMethodArgs = {"#paramOfDouble", "#typeOfLong", "#gid"})
    @Transactional(rollbackFor = {Exception.class})
    public void withWalkerTransactionManagement(String gid, boolean b, double paramOfDouble, long typeOfLong) {
        /**
         * do you understand ?
         *
         */
        System.out.println("I will rollback with my brother, too");
    }



    @Transactional
    public Participant diy_commit(String gid, boolean boolParam) {
        WalkerContext walkerContext = WalkerContextlManager.getContext();
        Map<String, Object> commitBody = Maps.newLinkedHashMap();
        commitBody.putIfAbsent("cancel_active_gid", gid);
        commitBody.putIfAbsent("根据需要填写你的参数1", boolParam);
        return Participant.create(walkerContext.getMasterGid(), walkerContext.getCurrentBranchGid(), "your_business_commit_url", commitBody, "something that need callback param from coordinator");
    }

    @Transactional
    public Participant diy_cancel(double paramOfDouble, long typeOfLong, String businessGid) {
        WalkerContext walkerContext = WalkerContextlManager.getContext();
        Map<String, Object> cancelBody = Maps.newLinkedHashMap();
        cancelBody.putIfAbsent("cancel_active_gid", businessGid);
        cancelBody.putIfAbsent("根据需要填写你的参数1", paramOfDouble);
        cancelBody.putIfAbsent("根据需要填写你的参数2", typeOfLong);
        return Participant.create(walkerContext.getMasterGid(), walkerContext.getCurrentBranchGid(), "your_business_cancel_url", cancelBody, "something that need callback param from coordinator");
    }

}

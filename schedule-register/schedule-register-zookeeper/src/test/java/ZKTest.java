import com.alibaba.fastjson.JSON;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

import java.util.List;

/**
 * @Description TODO
 * @Author beedoorwei
 * @Date 2020/6/24 22:26
 * @Version 1.0.0
 */
public class ZKTest {
    public static void main(String[] args) throws Exception{
        ZooKeeper zk = new ZooKeeper("127.0.0.1:2181",5000,null);
        Stat exists = zk.exists("/usr/local/dc/2", null);
        System.out.println(JSON.toJSONString(exists));
    }
}

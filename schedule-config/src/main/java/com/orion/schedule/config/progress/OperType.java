package com.orion.schedule.config.progress;

/**
 * @Description TODO
 * @Author beedoorwei
 * @Date 2019/12/17 21:58
 * @Version 1.0.0
 */
public enum OperType {
    BATCH_FINNISH(1), FETCH_FINISH(2), STOP(3);
    int v;

    OperType(int v) {
        this.v = v;
    }

    public static OperType ins(int v) {
        switch (v) {
            case 1:
                return BATCH_FINNISH;
            case 2:
                return FETCH_FINISH;
            case 3:
                return STOP;
        }
        return null;
    }

    public int getV() {
        return v;
    }
}

package com.hunter.game.models;

import android.util.Log;

import java.util.ArrayList;

/**
 * 存储游戏的状态，如是否开始，道具效果等.
 * Created by weiyan on 2016/11/13.
 */

public class GameState {

    /**
     * 没有这个游戏房间.
     */
    public static final int NO_SUCH_ROOM = 0;
    /**
     * 房间建立，未全部准备.
     */
    public static final int NOT_READY_YET = 1;
    /**
     * 房间建立且全部准备.
     */
    public static final int READY_TO_START = 2;
    /**
     * 游戏开始.
     */
    public static final int START = 3;
    /**
     * 游戏结束.
     */
    public static final int GAME_OVER = 4;

    /**
     * 存储游戏的状态：NO_SUCH_ROOM;NOT_READY_YET;READY_TO_START;START;GAME_OVER.
     */
    public int gameState;

    /**
     * 是否可以进行采集周围信号源的操作。否代表正在冷却.
     */
    public boolean isSearchButtonWake;


    /**
     * 以下三个变量分别表示信号的数据.
     * 对应是否被自己找到.
     * 以及当前探测器感受信号的大小(范围0.0f-1.0f)
     * 对于团队模式，还可以设置信号源的归属(未发现0,红队1,蓝队2)
     */
    public ArrayList<Signal> signals;
    public ArrayList<Boolean> isSignalsFound;
    public ArrayList<Float> signalSound;
    public ArrayList<Integer> signalBelong;

    /**
     * 当前获得的道具.可能为null(无道具).
     * 当前生效的道具.
     */
    public Item item;
    public ArrayList<Item> workingItems;//包括别人的debuff

    /**
     * 当前测向机的频率.取值范围1-6,参见Signal类.
     */
    private int presentFreq;
    /**
     * searchButton的冷却时间
     */
    float coldTime;

    public GameState(int gameState,ArrayList<Signal> signals) {
        this.gameState = gameState;
        isSearchButtonWake = true;
        presentFreq = Signal.FREQ_1;
        workingItems = new ArrayList<>();

        this.signals = signals;
        isSignalsFound = new ArrayList<>();
        signalSound = new ArrayList<>();
        signalBelong = new ArrayList<>();
        for (int i = 0; i < signals.size(); i++) {
            isSignalsFound.add(false);
            signalSound.add(0.0f);
            signalBelong.add(0);
            this.signals.get(i).setSoundMap(GameSetting.soundMap[i]);
        }
        coldTime = 2.0f;
    }
    /*
        计算距离
     */
    private static final  double EARTH_RADIUS = 6378137;//赤道半径(单位m)
    private static double rad(double d)
    {
        return d * Math.PI / 180.0;
    }
    public static double GetDistance(double lon1,double lat1,double lon2, double lat2)
    {
        double radLat1 = rad(lat1);
        double radLat2 = rad(lat2);
        double a = radLat1 - radLat2;
        double b = rad(lon1) - rad(lon2);
        double s = 2 * Math.asin(Math.sqrt(Math.pow(Math.sin(a/2),2)+Math.cos(radLat1)*Math.cos(radLat2)*Math.pow(Math.sin(b/2),2)));
        s = s * EARTH_RADIUS;
        //s = Math.round(s * 10000) / 10000;

        return s;
    }
    /**
     * 更新生效道具的剩余时间，更新信号源的声音大小.
     * @param deltaTime
     * signalSound
     * 同时负责统计已进行的时间
     * 更新状态
     */
    public void updateSound(float deltaTime,double latitude, double longitude,double angle) {
        for (Signal i:signals)
        {
            if(i.frequency==this.getFreq())
            {
                i.play(deltaTime);
            }
        }
        for(int i=0;i<this.signals.size();i++) {
            if(signals.get(i).frequency==this.getFreq()) {
                Signal aimSignal = signals.get(i);

                double len = GetDistance(aimSignal.longitude,aimSignal.latitude,longitude,latitude);
                double dx= GetDistance(aimSignal.longitude,latitude,longitude,latitude);
                if(latitude > aimSignal.latitude) {
                    dx = -dx;
                }
                double dangle=Math.asin(dx/len)*180/Math.PI;

                dangle = dangle - angle;

                if (len < 10){
                    aimSignal.setVolume(1f);
                }else if (len > 510){
                    aimSignal.setVolume(0f);
                }else{
                    float volume = 1 - ((float)len-10)/500;
                    volume *= 0.5f+(Math.cos(rad(dangle))*0.5);
                    aimSignal.setVolume(volume);
                }
            }
        }

        if(!isSearchButtonWake) {
            coldTime -= deltaTime;
            if (coldTime < 0) {
                isSearchButtonWake = true;
                coldTime = 2.0f;
            }
        }
    }

    /**
     * 从服务器获得了新的道具.
     * @param item
     */
    public void receiveItem(Item item) {
        if(item != null)
            this.item = item;
    }

    /**
     * 从服务器获得了新的附加状态.
     * @param item
     *
     */
    public void receiveAffect(Item item) {
        this.workingItems.add(item);
    }

    /**
     * 用户使用了道具.
     * 只有一个道具所以没有参数
     */
    public void useItem() {
        item = null;
    }

    /**
     * 用户点击了采集.更新采集状态并设searchButtonWake为false
     * @param latitude 当前纬度
     * @param longitude 当前经度
     * @return 返回采集成功的信号源的id,如果采集失败返回-1.
     */
    public int search(double latitude, double longitude) {

        isSearchButtonWake=false;

        int retnum=-1;
        for(int i = 0; i < signals.size(); i++){
            Signal aimSignal = signals.get(i);
            double dis=GetDistance(aimSignal.longitude,aimSignal.latitude,latitude,longitude);
            Log.d("distence",dis+"");
            if(dis<10)
            {
                if(!isSignalsFound.get(i)){
                    isSignalsFound.set(i, true);
                    retnum=i;
                    break;
                }
            }
        }
        return retnum;
    }

    public void addFreq() {
        presentFreq ++;
        if (presentFreq > 6) presentFreq = 1;
    }

    public int getFreq() {
        return presentFreq;
    }
}

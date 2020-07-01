package kim.hsl.bm.utils;

/**
 * Bitmap 内存缓存
 */
public class BitmapMemoryCache {
    private static final String TAG = "BitmapMemoryCache";

    /**
     * 单例实现
     */
    private static BitmapMemoryCache INSTANCE;
    private BitmapMemoryCache(){}
    public static BitmapMemoryCache getInstance(){
        if(INSTANCE == null){
            INSTANCE = new BitmapMemoryCache();
        }
        return INSTANCE;
    }
}

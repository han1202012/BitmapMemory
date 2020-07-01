package kim.hsl.bm.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.LruCache;

/**
 * Bitmap 内存缓存
 * 将图片缓存到内存中
 */
public class BitmapMemoryCache {
    private static final String TAG = "BitmapMemoryCache";

    /**
     * 应用上下文对象
     */
    private Context mContext;

    /**
     * 缓存图片的 LruCache
     */
    private LruCache<String, Bitmap> mLruCache;


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

    public void initCache(Context context){
        // 为成员变量赋值
        this.mContext = context;
        // 获取 Activity 管理器
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        // 获取应用可用的最大内存
        int maxMemory = activityManager.getMemoryClass();
        // 获取的 maxMemory 单位是 MB , 将其转为字节 , 除以 8
        int lruCacheMemoryByte = maxMemory / 8 * 1024 * 1024;

        // 设置的内存 , 一般是 APP 可用内存的 1/8
        new LruCache<String, Bitmap>(lruCacheMemoryByte){
            /**
             * 返回 LruCache 的键和值的大小 , 单位使用用户自定义的单位
             * 默认的实现中 , 返回 1 ; size 是 键值对个数 , 最大的 size 大小是最多键值对个数
             * 键值对条目在 LruCache 中缓存时 , 其大小不能改变
             * @param key
             * @param value
             * @return 返回 LruCache<String, Bitmap> 的值 , 即 Bitmap 占用内存
             */
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getByteCount();
            }

            /**
             * 从 LruCache 缓存移除 Bitmap 时会回调该方法
             * @param evicted
             * @param key
             * @param oldValue
             * @param newValue
             */
            @Override
            protected void entryRemoved(boolean evicted, String key, Bitmap oldValue, Bitmap newValue) {
                super.entryRemoved(evicted, key, oldValue, newValue);
            }
        };
    }

    /*
        下面的 3 个方法是提供给用户用于操作 LruCache 的接口
     */

    /**
     * 将键值对放入 LruCache 中
     * @param key
     * @param value
     */
    public void putBitmapToLruCache(String key, Bitmap value){
        mLruCache.put(key, value);
    }

    /**
     * 从 LruCache 中获取 Bitmap 对象
     * @param key
     * @return
     */
    public Bitmap getBitmapFromLruCache(String key){
        return mLruCache.get(key);
    }

    /**
     * 清除 LruCache 缓存
     */
    public void clearLruCache(){
        mLruCache.evictAll();
    }

}

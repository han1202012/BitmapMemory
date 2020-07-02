package kim.hsl.bm.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.util.LruCache;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

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
     * Bitmap 复用池
     * 使用 inBitmap 复用选项
     * 需要获取图片时 , 优先从 Bitmap 复用池中查找
     * 这里使用弱引用保存该 Bitmap , 每次 GC 时都会回收该 Bitmap
     * 创建一个线程安全的 HashSet , 其中的元素是 Bitmap 弱引用
     *
     * 该 Bitmap 复用池的作用是 , 假如 Bitmap 对象长时间不使用 , 就会从内存缓存中移除
     *
     * Bitmap 回收策略 :
     * 3.0 以下系统中 , Bitmap 内存在 Native 层
     * 3.0 以上系统中 , Bitmap 内存在 Java 层
     * 8.0 及以上的系统中 , Bitmap 内存在 Native 层
     *
     * 因此这里需要处理 Bitmap 内存在 Native 层的情况 , 监控到 Java 层的弱引用被释放了
     * 需要调用 Bitmap 对象的 recycle 方法 , 释放 Native 层的内存
     *
     * 需要使用引用队列监控弱引用的释放情况
     */
    Set<WeakReference<Bitmap>> bitmapReusePool;

    /**
     * 引用队列 , 用于监控 Set<WeakReference<Bitmap>> bitmapReusePool 的内存是否被回收
     * 需要维护一个线程 , 不断尝试从该引用队列中获取引用
     *
     */
    private ReferenceQueue<Bitmap> referenceQueue;

    /**
     * 监控 Set<WeakReference<Bitmap>> bitmapReusePool 的内存是否被回收 ,
     * 调用 ReferenceQueue<Bitmap> referenceQueue 的 remove 方法 ,
     * 查看是否存在被回收的弱引用 , 如果存在 , 直接回收该弱引用对应的 Bitmap 对象
     */
    private Thread referenceQueueMonitorThread;

    /**
     * 是否持续监控引用队列 ReferenceQueue
     */
    private boolean isMonitorReferenceQueue = true;



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

    /**
     * 使用时初始化
     * @param context
     */
    public void init(Context context){
        // 初始化内存缓存
        initLruCache(context);

        // 初始化弱引用队列
        initBitmapReusePool();
    }

    /**
     * 不使用时释放
     */
    public void release(){
        isMonitorReferenceQueue = false;
    }

    private void initLruCache(Context context){
        // 为成员变量赋值
        this.mContext = context;
        // 获取 Activity 管理器
        ActivityManager activityManager = (ActivityManager) context.getSystemService(
                Context.ACTIVITY_SERVICE);
        // 获取应用可用的最大内存
        int maxMemory = activityManager.getMemoryClass();
        // 获取的 maxMemory 单位是 MB , 将其转为字节 , 除以 8
        int lruCacheMemoryByte = maxMemory / 8 * 1024 * 1024;

        // 设置的内存 , 一般是 APP 可用内存的 1/8
        mLruCache = new LruCache<String, Bitmap>(lruCacheMemoryByte){
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
            protected void entryRemoved(boolean evicted, String key, Bitmap oldValue,
                                        Bitmap newValue) {
                super.entryRemoved(evicted, key, oldValue, newValue);

                /*
                    如果从 LruCache 内存缓存中移除的 Bitmap 是可变的
                    才能被复用 , 否则只能回收该 Bitmap 对象

                    Bitmap 回收策略 :
                    3.0 以下系统中 , Bitmap 内存在 Native 层
                    3.0 以上系统中 , Bitmap 内存在 Java 层
                    8.0 及以上的系统中 , Bitmap 内存在 Native 层

                    因此这里需要处理 Bitmap 内存在 Native 层的情况 , 监控到 Java 层的弱引用被释放了
                    需要调用 Bitmap 对象的 recycle 方法 , 释放 Native 层的内存
                 */
                if(oldValue.isMutable()){   // 可以被复用
                    // 将其放入弱引用中 , 每次 GC 启动后 , 如果该弱引用没有被使用 , 都会被回收
                    bitmapReusePool.add(new WeakReference<Bitmap>(oldValue, referenceQueue));
                }else{  // 不可被复用 , 直接回收
                    oldValue.recycle();
                }
            }
        };
    }

    private void initBitmapReusePool(){
        // 创建一个线程安全的 HashSet , 其中的元素是 Bitmap 弱引用
        bitmapReusePool = Collections.synchronizedSet(new HashSet<WeakReference<Bitmap>>());
        // 引用队列 , 当弱引用被 GC 扫描后 , 需要回收 , 会将该弱引用放入队列
        // 一直不断的尝试从该引用队列中获取数据 , 如果获取到数据 , 就要回收该对象
        referenceQueue = new ReferenceQueue<>();

        // 定义监控线程
        referenceQueueMonitorThread = new Thread(){
            @Override
            public void run() {
                while (isMonitorReferenceQueue){
                    try {
                        Reference<Bitmap> reference = (Reference<Bitmap>) referenceQueue.remove();
                        Bitmap bitmap = reference.get();
                        // 不为空 , 且没有被回收 , 回收 Bitmap 内存
                        if(bitmap != null && !bitmap.isRecycled()){
                            bitmap.recycle();
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        // 启动引用队列监控线程
        referenceQueueMonitorThread.start();
    }

    /**
     * 获取一个可以被复用的 Bitmap 对象
     * @param width
     * @param height
     * @param inSampleSize
     * @return
     */
    public Bitmap getReuseBitmap(int width,int height,int inSampleSize){
        // 3.0 之前的版本不启用 Bitmap 内存复用机制 , 返回 null 即可
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB){
            return null;
        }
        // 获取准备复用的 Bitmap , 之后设置到 Options 中
        Bitmap inBitmap = null;
        // 使用迭代器遍历该 Set 集合 , 如果遍历中涉及到删除 , 就要使用迭代器遍历
        Iterator<WeakReference<Bitmap>> iterator = bitmapReusePool.iterator();
        //迭代查找符合复用条件的Bitmap
        while (iterator.hasNext()){
            Bitmap bitmap = iterator.next().get();
            if (null != bitmap){
                //可以被复用
                /*if (checkInBitmap(bitmap,w,h,inSampleSize)){
                    inBitmap = bitmap;
                    //移出复用池
                    iterator.remove();
                    break;
                }*/
            }else{
                iterator.remove();
            }
        }
        return inBitmap;
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

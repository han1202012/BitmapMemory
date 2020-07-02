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
 * 在将图片缓存到 LruCache 内存中基础上 ,
 * 将从 LruCache 中移除的最近没有使用的 Bitmap 对象的内存复用
 * 这样能最大限度减少内存抖动
 */
public class BitmapLruCacheMemoryReuse {
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
    private static BitmapLruCacheMemoryReuse INSTANCE;
    private BitmapLruCacheMemoryReuse(){}
    public static BitmapLruCacheMemoryReuse getInstance(){
        if(INSTANCE == null){
            INSTANCE = new BitmapLruCacheMemoryReuse();
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
                // 如果使用的是复用的 Bitmap 对象 , 其占用内存大小是之前的图像分配的内存大小
                // 大于等于当前图像的内存占用大小
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    return value.getAllocationByteCount();
                }
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

    /**
     * 初始化引用队列
     */
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
     *
     * 与 BitmapFactory 配合使用 :
     *
     * Android 4.4 以后的 Bitmap 复用情况 :
     * 在 KITKAT ( Android 4.4 , 19 平台 ) 以后的代码中 ,
     * 只要被解码生成的 Bitmap 对象的字节大小 ( 缩放后的 )
     * 小于等于 inBitmap 的字节大小 , 就可以复用成功 ;
     *
     * Android 4.4 之前的 Bitmap 复用情况 : ( 比较苛刻 )
     * 在 KITKAT 之前的代码中 , 被解码的图像必须是
     *  - JPEG 或 PNG 格式 ,
     *  - 并且 图像大小必须是相等的 ,
     *  - inssampleSize 设置为 1 ,
     * 才能复用成功 ;
     * 另外被复用的图像的 像素格式 Config ( 如 RGB_565 ) 会覆盖设置的 inPreferredConfig 参数
     *
     * @param width
     * @param height
     * @param inSampleSize
     * @return
     */
    public Bitmap getReuseBitmap(int width,int height,int inSampleSize){
        // Android 2.3.3（API 级别 10）及以下的版本中 , 使用 Bitmap 对象的 recycle 方法回收内存
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.GINGERBREAD_MR1){
            // 如果 API 级别小于等于 10 , 不启用 Bitmap 内存复用机制 , 返回 null 即可
            return null;
        }
        // 获取准备复用的 Bitmap , 之后设置到 Options 中
        Bitmap inBitmap = null;
        // 使用迭代器遍历该 Set 集合 , 如果遍历中涉及到删除 , 就要使用迭代器遍历
        Iterator<WeakReference<Bitmap>> iterator = bitmapReusePool.iterator();
        //迭代查找符合复用条件的Bitmap
        while (iterator.hasNext()){
            // 循环遍历 Bitmap 对象
            Bitmap bitmap = iterator.next().get();
            if (bitmap != null){
                /*
                    检查该 Bitmap 对象是否可以达到复用要求 ,
                    如果达到复用要求 , 就取出这个 Bitmap 对象 , 并将其从队列中移除
                 */

                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN_MR2){
                    /*
                        Android 4.4（API 级别 19）以下的版本 : 在 Android 4.4（API 级别 19） 之前的代码中 ,
                        复用的前提是必须同时满足以下 3 个条件 :
                            1. 被解码的图像必须是 JPEG 或 PNG 格式
                            2. 被复用的图像宽高必须等于 解码后的图像宽高
                            3. 解码图像的 BitmapFactory.Options.inSampleSize 设置为 1 , 也就是不能缩放
                        才能复用成功 , 另外被复用的图像的像素格式 Config ( 如 RGB_565 ) 会覆盖设置的
                        BitmapFactory.Options.inPreferredConfig 参数 ;
                     */
                    if(bitmap.getWidth() == width &&
                            bitmap.getHeight() == height && //被复用的图像宽高必须等于 解码后的图像宽高
                            inSampleSize == 1){// 图像的 BitmapFactory.Options.inSampleSize 设置为 1
                        //符合要求
                        inBitmap = bitmap;
                        iterator.remove();
                    }
                }else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
                    /*
                        在 Android 4.4（API 级别 19）及以上的版本中 ,
                        只要被解码后的 Bitmap 对象的字节大小 , 小于等于 inBitmap 的字节大小 , 就可以复用成功 ;
                        解码后的乳香可以是缩小后的 , 即 BitmapFactory.Options.inSampleSize 可以大于1 ;
                     */

                    // 首先要计算图像的内存占用 , 先要计算出图像的宽高 , 如果图像需要缩放 , 计算缩放后的宽高
                    if(inSampleSize > 1){
                        width = width / inSampleSize ;
                        height = height / inSampleSize;
                    }

                    // 计算内存占用 , 默认 ARGB_8888 格式
                    int byteInMemory = width * height * 4;;
                    if(bitmap.getConfig() == Bitmap.Config.ARGB_8888){
                        // 此时每个像素占 4 字节
                        byteInMemory = width * height * 4;

                    }else if(bitmap.getConfig() == Bitmap.Config.RGB_565){
                        // 此时每个像素占 2 字节
                        byteInMemory = width * height * 2;
                    }

                    // 如果解码后的图片内存小于等于被复用的内存大小 , 可以复用
                    if(byteInMemory <= bitmap.getAllocationByteCount()){
                        //符合要求
                        inBitmap = bitmap;
                        iterator.remove();
                    }

                }

            }else if( bitmap == null ){
                // 如果 bitmap 为空 , 直接从复用 Bitmap 集合中移除
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

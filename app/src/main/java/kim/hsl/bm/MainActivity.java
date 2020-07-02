package kim.hsl.bm;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.TextView;

import kim.hsl.bm.utils.BitmapDiskLruCacheMemoryReuse;
import kim.hsl.bm.utils.BitmapLruCacheMemoryReuse;
import kim.hsl.bm.utils.BitmapSizeReduce;

public class MainActivity extends AppCompatActivity {

    static {
        System.loadLibrary("native-lib");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 动态获取权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            initPermissions();
        }

        TextView tv = findViewById(R.id.sample_text);
        tv.setText(stringFromJNI());

        // 查看 Bitmap 内存占用情况
        //showBitmapMeory();

        // 缩小图像尺寸
        //sizeReduce();

        // 内存缓存
        //memoryCache();

        // 磁盘内存缓存
        diskMemoryCache();
    }

    /**
     * 图像缓存
     */
    private void diskMemoryCache(){
        // 初始化 LruCache 内存缓存 , 与引用队列 , 一般在 onCreate 方法中初始化
        // 这里为了演示 , 放在方法的开头位置
        BitmapDiskLruCacheMemoryReuse.getInstance().init(this, Environment.getExternalStorageDirectory() + "/diskCache");

        // 1. 第一次尝试从 LruCache 内存中获取 Bitmap 数据
        Bitmap bitmap = BitmapDiskLruCacheMemoryReuse.getInstance().
                getBitmapFromLruCache(R.drawable.blog + "");

        /*
            如果从内存中获取 Bitmap 对象失败 , 再次从磁盘中尝试获取该 Bitmap
         */
        if(bitmap == null){
            // 要复用内存的 Bitmap 对象 , 将新的 Bitmap 写入到该 Bitmap 内存中
            Bitmap inBitmap = null;
            // 尝试获取复用对象
            BitmapDiskLruCacheMemoryReuse.getInstance().
                    getReuseBitmap(200, 200, 1);

            // 2. 第二次尝试从磁盘中获取图片
            bitmap = BitmapDiskLruCacheMemoryReuse.getInstance().getBitmapFromDisk(
                    R.drawable.blog + "", inBitmap);


            // 磁盘中没有找到 , 再次尝试加载该图片
            if(bitmap == null) {
                // 3. 如果内存, 磁盘都没有获取到 Bitmap, 那么加载指定大小格式的图像
                bitmap = BitmapSizeReduce.getResizedBitmap(this, R.drawable.blog,
                        200, 200, false, inBitmap);

                // 将新的 bitap 放入 LruCache 内存缓存中
                BitmapDiskLruCacheMemoryReuse.getInstance().
                        putBitmapToLruCache(R.drawable.blog + "", bitmap);
            }

        }
    }



    /**
     * 图像缓存
     */
    private void memoryCache(){
        // 初始化 LruCache 内存缓存 , 与引用队列 , 一般在 onCreate 方法中初始化
        // 这里为了演示 , 放在方法的开头位置
        BitmapLruCacheMemoryReuse.getInstance().init(this);

        // 第一次从 LruCache 内存中获取 Bitmap 数据
        Bitmap bitmap = BitmapLruCacheMemoryReuse.getInstance().
                getBitmapFromLruCache(R.drawable.blog + "");

        /*
            如果从内存中获取 Bitmap 对象失败 , 这里就需要创建该图片 , 并放入 LruCache 内存中
         */
        if(bitmap == null){
            // 要复用内存的 Bitmap 对象 , 将新的 Bitmap 写入到该 Bitmap 内存中
            Bitmap inBitmap = null;
            // 尝试获取复用对象
            BitmapLruCacheMemoryReuse.getInstance().
                    getReuseBitmap(200, 200, 1);
            // 加载指定大小格式的图像
            bitmap = BitmapSizeReduce.getResizedBitmap(this, R.drawable.blog,
                    200, 200, false, inBitmap);

            // 将新的 bitap 放入 LruCache 内存缓存中
            BitmapLruCacheMemoryReuse.getInstance().
                    putBitmapToLruCache(R.drawable.blog + "", bitmap);

            Log.i("Bitmap 没有获取到创建新的", "blog : " + bitmap.getWidth() + " , " +
                    bitmap.getHeight() + " , " +
                    bitmap.getByteCount());

        }else{
            Log.i("Bitmap 内存中获取数据", "blog : " + bitmap.getWidth() + " , " +
                    bitmap.getHeight() + " , " +
                    bitmap.getByteCount());
        }



        // 第一次从 LruCache 内存中获取 Bitmap 数据
        Bitmap bitmap2 = BitmapLruCacheMemoryReuse.getInstance().
                getBitmapFromLruCache(R.drawable.blog + "");

        Log.i("Bitmap 第二次内存中获取数据", "blog : " + bitmap2.getWidth() + " , " +
                bitmap2.getHeight() + " , " +
                bitmap2.getByteCount());
    }


    /**
     * 图像尺寸缩小
     */
    private void sizeReduce(){
        // 从资源文件中加载内存
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.blog);
        // 打印 Bitmap 对象的宽高, 字节大小
        Log.i("Bitmap", "blog : " + bitmap.getWidth() + " , " +
                bitmap.getHeight() + " , " +
                bitmap.getByteCount());

        // 从资源文件中加载内存
        Bitmap reduceSizeBitmap = BitmapSizeReduce.getResizedBitmap(this, R.drawable.blog,
                100, 100 , false , null);
        // 打印 Bitmap 对象的宽高, 字节大小
        Log.i("Bitmap", "reduceSizeBitmap : " + reduceSizeBitmap.getWidth() + " , " +
                reduceSizeBitmap.getHeight() + " , " +
                reduceSizeBitmap.getByteCount());
    }


    /**
     * 分析 Bitmap 内存占用情况
     */
    private void showBitmapMeory(){
        Log.i("Bitmap", "getResources().getDisplayMetrics().densityDpi : " +
                getResources().getDisplayMetrics().densityDpi +
                " , getResources().getDisplayMetrics().density : " +
                getResources().getDisplayMetrics().density);

        // 从资源文件中加载内存
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.blog);
        // 打印 Bitmap 对象的宽高, 字节大小
        Log.i("Bitmap", "blog : " + bitmap.getWidth() + " , " +
                bitmap.getHeight() + " , " +
                bitmap.getByteCount());

        bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.blog_h);
        // 打印 Bitmap 对象的宽高, 字节大小
        Log.i("Bitmap", "blog_h : " + bitmap.getWidth() + " , " +
                bitmap.getHeight() + " , " +
                bitmap.getByteCount());

        bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.blog_m);
        // 打印 Bitmap 对象的宽高, 字节大小
        Log.i("Bitmap", "blog_m : " + bitmap.getWidth() + " , " +
                bitmap.getHeight() + " , " +
                bitmap.getByteCount());


        bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.blog_x);
        // 打印 Bitmap 对象的宽高, 字节大小
        Log.i("Bitmap", "blog_x : " + bitmap.getWidth() + " , " +
                bitmap.getHeight() + " , " +
                bitmap.getByteCount());

        bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.blog_xx);
        // 打印 Bitmap 对象的宽高, 字节大小
        Log.i("Bitmap", "blog_xx : " + bitmap.getWidth() + " , " +
                bitmap.getHeight() + " , " +
                bitmap.getByteCount());

        bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.blog_xxx);
        // 打印 Bitmap 对象的宽高, 字节大小
        Log.i("Bitmap", "blog_xxx : " + bitmap.getWidth() + " , " +
                bitmap.getHeight() + " , " +
                bitmap.getByteCount());
    }

    public native String stringFromJNI();



    /**
     * 需要获取的权限列表
     */
    private String[] permissions = new String[]{
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    /**
     * 动态申请权限的请求码
     */
    private static final int PERMISSION_REQUEST_CODE = 888;

    /**
     * 动态申请权限
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void initPermissions() {
        if (isLacksPermission()) {
            //动态申请权限 , 第二参数是请求吗
            requestPermissions(permissions, PERMISSION_REQUEST_CODE);
        }
    }

    /**
     * 判断是否有 permissions 中的权限
     * @return
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    public boolean isLacksPermission() {
        for (String permission : permissions) {
            if(checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED){
                return true;
            }
        }
        return false;
    }
}

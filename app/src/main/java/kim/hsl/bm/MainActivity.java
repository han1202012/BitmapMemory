package kim.hsl.bm;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

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

        TextView tv = findViewById(R.id.sample_text);
        tv.setText(stringFromJNI());

        // 查看 Bitmap 内存占用情况
        //showBitmapMeory();

        // 缩小图像尺寸
        //sizeReduce();

        // 内存缓存
        memoryCache();

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
}

package kim.hsl.bm;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import kim.hsl.bm.utils.BitmapMemoryCache;
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


    }


    /**
     * 图像缓存
     */
    private void memoryCache(){
        // 初始化 LruCache 内存缓存
        BitmapMemoryCache.getInstance().initLruCache(this);
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

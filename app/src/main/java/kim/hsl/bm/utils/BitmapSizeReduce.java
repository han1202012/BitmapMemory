package kim.hsl.bm.utils;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

/**
 * Bitmap 尺寸缩小
 */
public class BitmapSizeReduce {
    private static final String TAG = "BitmapSizeReduce";

    /**
     * 获取指定尺寸于鏊求的 Bitmap 对象
     *
     * @param context           上下文对象
     * @param iamgeResId        要解析的图片资源 id
     * @param maxBitmapWidth    Bitmap 的最大宽度
     * @param maxBitmapHeight   Bitmap 的最大高度
     * @param hasAlphaChannel   是否包含 ALPHA 通道, 即透明度信息
     * @param inBitmap          复用的 Bitmap, 将新的 Bitmap 对象解析到该 Bitmap 内存中
     * @return  返回新的 Bitmap 对象
     */
    public static Bitmap getResizedBitmap(Context context,
                                          int iamgeResId, int maxBitmapWidth, int maxBitmapHeight,
                                          boolean hasAlphaChannel, Bitmap inBitmap){

        // 0. 声明方法中使用的局部变量

        // 用于解析资源
        Resources resources = context.getResources();
        // Bitmap 图片加载选项
        BitmapFactory.Options options = new BitmapFactory.Options();
        // 图片宽度
        int imageWidth;
        // 图片高度
        int imageHeight;
        /*
            根据 图片宽度 imageWidth , 图片高度 imageHeight ,
            最大宽度 maxBitmapWidth , 最大高度 maxBitmapHeight ,
            计算出的图片缩放系数 , 该值最终要设置到 BitmapFactory.Options 对象中
         */
        int inSampleSize = 1;

        // 1. 解析图片参数 : 该阶段不解析所有的数据 , 否则会将实际的图片数据解析到内存中 , 这里只解析图片的宽高信息

        /*
            设置 inJustDecodeBounds 为 true , 解析器会返回 null
            但是 outXxx 字段会被设置对应的图片属性值 ,
            如 : outWidth 输出图像的 宽度 , outHeight 输出高度 , outMimeType 输出类型 ,
            outConfig 像素格式 , outColorSpace 输出颜色空间
         */
        options.inJustDecodeBounds = true;

        /*
            由于设置了 inJustDecodeBounds = true , 该方法返回值为空 ;
            但是传入的 BitmapFactory.Options 对象中的 outXxx 字段都会被赋值 ;
            如 outWidth , outHeight , outConfig , outColorSpace 等 ;
            可以获取该图片的宽高 , 像素格式 , 颜色空间等信息
         */
        BitmapFactory.decodeResource(resources, iamgeResId, options);

        // 获取 iamgeResId 图片资源对应的图片宽度
        imageWidth = options.outWidth;
        // 获取 iamgeResId 图片资源对应的图片高度
        imageHeight = options.outHeight;


        // 2. 计算图片缩小比例

        /*
            计算缩小的比例
            宽度和高度只要存在一个大于限定的最大值时 , 就进行缩小操作
            要求指定的图片必须能放到 maxBitmapWidth 宽度 , maxBitmapHeight 高度的矩形框中
            最终要求就是 宽度必须小于 maxBitmapWidth, 同时高度也要小于 maxBitmapHeight
         */
        if(imageWidth > maxBitmapWidth || imageHeight > maxBitmapHeight){
            // 如果需要启动缩小功能 , 那么进入如下循环 , 试探最小的缩放比例是多少
            while ( imageWidth / inSampleSize > maxBitmapWidth ||
                    imageHeight / inSampleSize > maxBitmapHeight ){
                // 注意该值必须是 2 的幂次方值 , 1 , 2 , 4 , 8 , 16 , 32
                inSampleSize = inSampleSize * 2;
            }

            // 执行到此处 , 说明已经找到了最小的缩放比例 , 打印下最小比例
            Log.w(TAG, "getResizedBitmap inSampleSize=" + inSampleSize);
        }


        // 3. 设置真实的解析参数
        /*
            inSampleSize 设置大于 1 : 如果值大于 1 , 那么就会缩小图片 ;
            解码器操作 : 此时解码器对原始的图片数据进行子采样 , 返回较小的 Bitmap 对象 ;

            样本个数 : 样本的大小是在两个维度计算的像素个数 , 每个像素对应一个解码后的图片中的单独的像素点 ;
            样本个数计算示例 :
            如果 inSampleSize 值为 2 , 那么宽度的像素个数会缩小 2 倍 , 高度也会缩小两倍 ;
            整体像素个数缩小 4 倍 , 内存也缩小了 4 倍 ;

            小于 1 取值 : 如果取值小于 1 , 那么就会被当做 1 , 1 相当于 2 的 0 次方 ;
            取值要求 : 该值必须是 2 的幂次方值 , 2 的次方值 , 如 1 , 2 , 4 , 8 , 16 , 32
            如果出现了不合法的值 , 就会就近四舍五入到最近的 2 的幂次方值
         */
        options.inSampleSize = inSampleSize;


        if (!hasAlphaChannel){
            options.inPreferredConfig = Bitmap.Config.RGB_565;
        }
        options.inJustDecodeBounds = false;
        // 需要设置为异变的才能够被复用内存
        options.inMutable = true;
        options.inBitmap = inBitmap;
        return BitmapFactory.decodeResource(resources,iamgeResId,options);


        return null;
    }

}

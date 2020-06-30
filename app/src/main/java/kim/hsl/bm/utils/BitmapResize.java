package kim.hsl.bm.utils;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

/**
 * Bitmap 尺寸修改
 */
public class BitmapResize {

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
        int inSampleSize;

        // 1. 初步解析 : 该阶段不解析所有的数据 , 否则会将实际的图片数据解析到内存中 , 这里只解析图片的宽高信息

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


        // 2. 设置真实的解析参数



        return null;
    }

}

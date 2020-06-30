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
                // 注意该值必须是 2 的幂次方值 , 1 , 2 , 4 , 8 , 16 , 32 , 64
                inSampleSize = inSampleSize * 2;
            }

            // 执行到此处 , 说明已经找到了最小的缩放比例 , 打印下最小比例
            Log.w(TAG, "getResizedBitmap inSampleSize=" + inSampleSize);
        }


        // 3. 设置图像解码参数

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

        // 用户设置的是否保留透明度选项 , 如果不保留透明度选项 , 设置像素格式为 RGB_565
        // 每个像素占 2 字节内存
        if (!hasAlphaChannel){
            /*
                指定配置解码 : 如果配置为非空 , 解码器会将 Bitmap 的像素解码成该指定的非空像素格式 ;
                自动匹配配置解码 : 如果该配置为空 , 或者像素配置无法满足 , 解码器会尝试根据系统的屏幕深度 ,
                源图像的特点 , 选择合适的像素格式 ;
                如果源图像有透明度通道 , 那么自动匹配的默认配置也有对应通道 ;
                默认配置 : 默认使用 ARGB_8888 进行解码
             */
            options.inPreferredConfig = Bitmap.Config.RGB_565;
        }

        /*
            注意解码真实图像的时候 , 要将 inJustDecodeBounds 设置为 false
            否则将不会解码 Bitmap 数据 , 只会将
            outWidth , outHeight , outConfig , outColorSpace 等 outXxx 图片参数解码出来
         */
        options.inJustDecodeBounds = false;

        /*
            设置图片可以被复用
         */
        options.inMutable = true;

        /*
            如果设置了一个 Bitmap 对象给 inBitmap 参数
            解码方法会获取该 Bitmap 对象 , 当加载图片内容时 , 会尝试复用该 Bitmap 对象的内存

            如果解码方法无法复用该 Bitmap 对象 , 解码方法可能会抛出 IllegalArgumentException 异常 ;
            当前的实现是很有必要的 , 被复用的图片必须是可变的 , 解码后的 Bitmap 对象也是可变的 ,
            即使当解码一个资源图片时 , 经常会得到一个不可变的 Bitmap 对象 ;

            确保是否解码成功 :
            该解码方法返回的 Bitmap 对象是可以使用的 ,
            鉴于上述约束情况 和 可能发生的失败故障 , 不能假定该图片解码操作是成功的 ;

            检查解码返回的 Bitmap 对象是否与设置给 Options 对象的 inBitmap 相匹配 ,
            来判断该 inBitmap 是否被复用 ;

            不管有没有复用成功 , 你应该使用解码函数返回的 Bitmap 对象 , 保证程序的正常运行 ;

            与 BitmapFactory 配合使用 :

            在 KITKAT 以后的代码中 , 只要被解码生成的 Bitmap 对象的字节大小 ( 缩放后的 )
            小于等于 inBitmap 的字节大小 , 就可以复用成功 ;

            在 KITKAT 之前的代码中 , 被解码的图像必须是
            JPEG 或 PNG 格式 ,
            并且 图像大小必须是相等的 ,
            inssampleSize 设置为 1 ,
            才能复用成功 ;
            另外被复用的图像的 像素格式 Config ( 如 RGB_565 ) 会覆盖设置的 inPreferredConfig 参数
         */
        options.inBitmap = inBitmap;


        // 4. 解码图片 , 并返回被解码的图片

        return BitmapFactory.decodeResource(resources, iamgeResId, options);
    }

}

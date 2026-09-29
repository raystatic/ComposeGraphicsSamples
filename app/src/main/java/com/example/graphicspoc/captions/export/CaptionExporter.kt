package com.example.graphicspoc.captions.export

import android.content.ContentValues
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PorterDuff
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.CanvasOverlay
import androidx.media3.effect.OverlayEffect
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import com.example.graphicspoc.captions.model.CaptionLine
import com.example.graphicspoc.captions.style.CaptionLayout
import com.example.graphicspoc.captions.style.CaptionRenderer
import com.example.graphicspoc.captions.style.CaptionStyle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume

/**
 * Burns captions into the video with Media3 Transformer. The audio track is copied as-is and
 * the video is re-encoded to H.264 with a [CanvasOverlay] that runs [CaptionRenderer] per frame.
 */
@OptIn(UnstableApi::class)
class CaptionExporter(private val context: Context) {

    /** Must be called from the main thread (Transformer requirement). */
    suspend fun export(
        videoUri: Uri,
        lines: List<CaptionLine>,
        style: CaptionStyle,
        layout: CaptionLayout,
        onProgress: (Float) -> Unit,
    ): Uri {
        val outFile = File(context.cacheDir, "captioned_${System.currentTimeMillis()}.mp4")
        try {
            renderToFile(videoUri, outFile, lines, style, layout, onProgress)
            return withContext(Dispatchers.IO) { saveToGallery(outFile) }
        } finally {
            outFile.delete()
        }
    }

    private suspend fun renderToFile(
        videoUri: Uri,
        outFile: File,
        lines: List<CaptionLine>,
        style: CaptionStyle,
        layout: CaptionLayout,
        onProgress: (Float) -> Unit,
    ) = coroutineScope {
        val overlay = object : CanvasOverlay(/* useInputFrameSize= */ true) {
            private val renderer = CaptionRenderer()

            override fun onDraw(canvas: Canvas, presentationTimeUs: Long) {
                // The bitmap is reused between frames, so wipe the previous caption first.
                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
                renderer.draw(
                    canvas, canvas.width.toFloat(), canvas.height.toFloat(),
                    presentationTimeUs / 1000, lines, style, layout,
                )
            }
        }
        val edited = EditedMediaItem.Builder(MediaItem.fromUri(videoUri))
            .setEffects(Effects(emptyList(), listOf(OverlayEffect(listOf(overlay)))))
            .build()

        val done = CompletableDeferred<Unit>()
        val transformer = Transformer.Builder(context)
            .setVideoMimeType(MimeTypes.VIDEO_H264)
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    done.complete(Unit)
                }

                override fun onError(
                    composition: Composition,
                    exportResult: ExportResult,
                    exportException: ExportException,
                ) {
                    done.completeExceptionally(exportException)
                }
            })
            .build()

        transformer.start(edited, outFile.absolutePath)
        val poller = launch {
            val holder = ProgressHolder()
            while (true) {
                if (transformer.getProgress(holder) == Transformer.PROGRESS_STATE_AVAILABLE) {
                    onProgress(holder.progress / 100f)
                }
                delay(250)
            }
        }
        try {
            done.await()
        } catch (e: CancellationException) {
            transformer.cancel()
            throw e
        } finally {
            poller.cancel()
        }
    }

    private suspend fun saveToGallery(file: File): Uri {
        val name = "hinglish_captions_${System.currentTimeMillis()}.mp4"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, name)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/HinglishCaptions")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
                ?: error("Could not create gallery entry")
            resolver.openOutputStream(uri)!!.use { out -> file.inputStream().use { it.copyTo(out) } }
            values.clear()
            values.put(MediaStore.Video.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            return uri
        }
        @Suppress("DEPRECATION")
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "HinglishCaptions")
        dir.mkdirs()
        val dest = File(dir, name)
        file.copyTo(dest, overwrite = true)
        return scan(dest) ?: Uri.fromFile(dest)
    }

    /** Registers the file with the gallery and returns its content:// uri (shareable). */
    private suspend fun scan(file: File): Uri? = suspendCancellableCoroutine { cont ->
        MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), arrayOf("video/mp4")) { _, uri ->
            if (cont.isActive) cont.resume(uri)
        }
    }
}

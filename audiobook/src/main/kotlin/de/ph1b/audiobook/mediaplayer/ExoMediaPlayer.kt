package de.ph1b.audiobook.mediaplayer

import android.content.Context
import android.net.Uri
import com.google.android.exoplayer.ExoPlaybackException
import com.google.android.exoplayer.ExoPlayer
import com.google.android.exoplayer.extractor.ExtractorSampleSource
import com.google.android.exoplayer.upstream.DefaultAllocator
import com.google.android.exoplayer.upstream.DefaultUriDataSource
import de.ph1b.audiobook.playback.SpeedRenderer
import rx.Observable
import rx.subjects.PublishSubject

/**
 * TODO: Class description
 *
 * @author Paul Woitaschek
 */
class ExoMediaPlayer(private val context: Context) : MediaPlayerInterface {

    private val exoPlayer = ExoPlayer.Factory.newInstance(1);

    init {
        exoPlayer.addListener(object : ExoPlayer.Listener {
            override fun onPlayerError(error: ExoPlaybackException?) {
                errorSubject.onNext(Unit)
            }

            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                if (!playWhenReady && playbackState == ExoPlayer.STATE_ENDED) {
                    completionSubject.onNext(Unit)
                }
            }

            override fun onPlayWhenReadyCommitted() {

            }
        })
    }

    override fun release() {
        exoPlayer.release()
    }

    override fun start() {
        exoPlayer.playWhenReady = true
    }

    override fun reset() {
        exoPlayer.playWhenReady = false
    }


    private val BUFFER_SEGMENT_SIZE = 64 * 1024;
    private val BUFFER_SEGMENT_COUNT = 256;

    var audioRenderer: SpeedRenderer? = null

    override fun prepare() {
        val uri = Uri.parse("file://$dataSource");
        val userAgent = "Paul"
        val allocator = DefaultAllocator(BUFFER_SEGMENT_SIZE)
        val dataSource = DefaultUriDataSource(context, null, userAgent);
        val sampleSource = ExtractorSampleSource(uri, dataSource, allocator, BUFFER_SEGMENT_COUNT * BUFFER_SEGMENT_SIZE);
        audioRenderer = SpeedRenderer(sampleSource);
        audioRenderer!!.playbackSpeed = playbackSpeed
        exoPlayer.prepare(audioRenderer);
    }

    override var currentPosition: Int
        get() = exoPlayer.currentPosition.toInt()
        set(value) {
            exoPlayer.seekTo(value.toLong())
        }

    override fun pause() {
        exoPlayer.playWhenReady = false
    }

    override var playbackSpeed: Float = 1F
        set(value) {
            audioRenderer?.playbackSpeed = value
            field = value
        }

    private var dataSource: String? = null

    override fun setDataSource(source: String) {
        dataSource = source
    }

    private val errorSubject = PublishSubject.create<Unit>()

    override val errorObservable: Observable<Unit> = errorSubject.asObservable()

    private val completionSubject = PublishSubject.create<Unit>()

    override val completionObservable: Observable<Unit> = completionSubject.asObservable()

    override fun setWakeMode(context: Context, mode: Int) {

    }

    override val duration: Int
        get() = exoPlayer.duration.toInt()
}
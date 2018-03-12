package me.cooper.rick.crowdcontrollerclient.util

import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

fun <T> Flowable<T>.subscribeWithConsumers(successConsumer: (T) -> Unit,
                                           failureConsumer: (Throwable) -> Unit) {
    subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(successConsumer , failureConsumer)
}

fun <T> Observable<T>.call(successConsumer: (T) -> Unit,
                           failureConsumer: (Throwable) -> Unit) {
    subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(successConsumer , failureConsumer)
}

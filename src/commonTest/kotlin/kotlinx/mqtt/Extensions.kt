package kotlinx.mqtt

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi


@ExperimentalCoroutinesApi
fun <T> Deferred<T>.awaitSync(): T {
    while (!isCompleted) {
        continue
    }
    return getCompleted()
}

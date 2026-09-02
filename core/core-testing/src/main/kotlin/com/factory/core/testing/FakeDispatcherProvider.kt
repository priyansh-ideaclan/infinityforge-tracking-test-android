package com.factory.core.testing

import com.factory.core.common.DispatcherProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher

/** All three dispatchers point at the same [TestDispatcher] for deterministic tests. */
@OptIn(ExperimentalCoroutinesApi::class)
class FakeDispatcherProvider(
    dispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : DispatcherProvider {
    override val io = dispatcher
    override val default = dispatcher
    override val main = dispatcher
}

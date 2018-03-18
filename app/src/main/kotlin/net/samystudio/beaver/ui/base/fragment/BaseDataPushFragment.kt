@file:Suppress("unused", "MemberVisibilityCanBePrivate", "UNUSED_PARAMETER")

package net.samystudio.beaver.ui.base.fragment

import android.arch.lifecycle.Observer
import android.os.Bundle
import net.samystudio.beaver.data.remote.CompletableRequestState
import net.samystudio.beaver.ui.base.viewmodel.BaseFragmentViewModel
import net.samystudio.beaver.ui.base.viewmodel.DataPushViewModel

abstract class BaseDataPushFragment<VM> :
    BaseFragment<VM>() where VM : BaseFragmentViewModel, VM : DataPushViewModel
{
    override fun onViewModelCreated(savedInstanceState: Bundle?)
    {
        super.onViewModelCreated(savedInstanceState)

        viewModel.dataPushCompletable.observe(this, Observer {
            it?.let {
                when (it)
                {
                    is CompletableRequestState.Start    -> dataPushStart()
                    is CompletableRequestState.Complete ->
                    {
                        dataPushSuccess()
                        dataPushTerminate()
                    }
                    is CompletableRequestState.Error    ->
                    {
                        dataPushError(it.error)
                        dataPushTerminate()
                    }
                }
            }
        })
    }

    protected abstract fun dataPushStart()
    protected abstract fun dataPushSuccess()
    protected abstract fun dataPushError(throwable: Throwable)
    protected abstract fun dataPushTerminate()
}

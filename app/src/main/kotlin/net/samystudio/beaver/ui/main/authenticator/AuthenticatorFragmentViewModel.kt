package net.samystudio.beaver.ui.main.authenticator

import android.accounts.AccountManager
import android.app.Activity
import android.arch.lifecycle.LiveData
import android.content.Intent
import android.os.Bundle
import io.reactivex.Observable
import net.samystudio.beaver.BuildConfig
import net.samystudio.beaver.data.remote.CompletableRequestState
import net.samystudio.beaver.data.remote.api.AuthenticatorApiInterfaceManager
import net.samystudio.beaver.di.scope.FragmentScope
import net.samystudio.beaver.ui.base.viewmodel.BaseFragmentViewModel
import net.samystudio.beaver.ui.base.viewmodel.DataPushViewModel
import net.samystudio.beaver.ui.common.viewmodel.CompletableRequestLiveData
import net.samystudio.beaver.ui.main.MainActivityViewModel
import javax.inject.Inject

@FragmentScope
class AuthenticatorFragmentViewModel
@Inject
constructor(activityViewModel: MainActivityViewModel,
            private val authenticatorApiInterfaceManager: AuthenticatorApiInterfaceManager) :
    BaseFragmentViewModel(activityViewModel), DataPushViewModel
{
    private val _dataPushCompletable: CompletableRequestLiveData = CompletableRequestLiveData()
    override val dataPushCompletable: LiveData<CompletableRequestState> = _dataPushCompletable
    override val title: String?
        get() = null

    fun <T : AuthenticatorUserFlow> addUserFlow(observable: Observable<T>)
    {
        disposables.add(observable.flatMap { userFlow ->
            when (userFlow)
            {
                is AuthenticatorUserFlow.SignIn ->
                    _dataPushCompletable.bind(
                        authenticatorApiInterfaceManager.signIn(userFlow.email,
                                                                userFlow.password)).doOnNext(
                        {
                            if (it is CompletableRequestState.Complete)
                                handleSignResult(userFlow.email, userFlow.password)
                        })
                is AuthenticatorUserFlow.SignUp ->
                    _dataPushCompletable.bind(
                        authenticatorApiInterfaceManager.signUp(userFlow.email,
                                                                userFlow.password)).doOnNext(
                        {
                            if (it is CompletableRequestState.Complete)
                                handleSignResult(userFlow.email, userFlow.password)
                        })
                else                            ->
                    Observable.just(Observable.error<Unit> {
                        IllegalArgumentException("Unknown user flow ${userFlow.javaClass.name}.")
                    })
            }
        }.subscribe())
    }

    private fun handleSignResult(email: String, password: String)
    {
        val bundle = Bundle()
        bundle.putString(AccountManager.KEY_ACCOUNT_NAME, email)
        bundle.putString(AccountManager.KEY_ACCOUNT_TYPE, BuildConfig.APPLICATION_ID)
        bundle.putString(AccountManager.KEY_PASSWORD, password)

        val intent = Intent()
        intent.putExtras(bundle)
        setResult(Activity.RESULT_OK, intent)
    }
}
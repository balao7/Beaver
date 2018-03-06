package net.samystudio.beaver.ui

import dagger.Module
import dagger.android.ContributesAndroidInjector
import net.samystudio.beaver.di.scope.ActivityScope
import net.samystudio.beaver.ui.authenticator.AuthenticatorActivity
import net.samystudio.beaver.ui.authenticator.AuthenticatorActivityModule
import net.samystudio.beaver.ui.main.MainActivity
import net.samystudio.beaver.ui.main.MainActivityModule

@Module
abstract class ActivityBuilderModule
{

    @ContributesAndroidInjector(modules = [MainActivityModule::class])
    @ActivityScope
    abstract fun contributeMainActivity(): MainActivity

    @ContributesAndroidInjector(modules = [AuthenticatorActivityModule::class])
    @ActivityScope
    abstract fun contributeAuthenticatorActivity(): AuthenticatorActivity
}
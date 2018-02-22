package net.samystudio.beaver.ui.main

import android.arch.lifecycle.LifecycleOwner
import android.support.v4.app.Fragment
import dagger.Binds
import dagger.Module
import net.samystudio.beaver.di.scope.FragmentScope
import net.samystudio.beaver.ui.base.fragment.BaseFragmentModule

@Module(includes = [BaseFragmentModule::class])
abstract class MainFragmentModule
{
    @Binds
    @FragmentScope
    abstract fun bindFragment(fragment: MainFragment): Fragment

    @Binds
    @FragmentScope
    abstract fun bindLifeCycleOwner(fragment: MainFragment): LifecycleOwner

    /*@Binds
    @IntoMap
    @ViewModelKey(MainViewModel::class)
    @PerFragment
    abstract fun bindMainViewModel(model: MainViewModel): ViewModel*/
}

/**
 * Wire
 * Copyright (C) 2016 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.waz.zclient.pages.main;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.waz.api.CredentialsUpdateListener;
import com.waz.api.IConversation;
import com.waz.api.Message;
import com.waz.api.User;
import com.waz.zclient.OnBackPressedListener;
import com.waz.zclient.R;
import com.waz.zclient.controllers.accentcolor.AccentColorObserver;
import com.waz.zclient.controllers.confirmation.ConfirmationObserver;
import com.waz.zclient.controllers.confirmation.ConfirmationRequest;
import com.waz.zclient.controllers.confirmation.IConfirmationController;
import com.waz.zclient.controllers.giphy.GiphyObserver;
import com.waz.zclient.controllers.navigation.NavigationControllerObserver;
import com.waz.zclient.controllers.navigation.Page;
import com.waz.zclient.controllers.onboarding.OnboardingControllerObserver;
import com.waz.zclient.controllers.singleimage.SingleImageObserver;
import com.waz.zclient.controllers.usernames.UsernamesControllerObserver;
import com.waz.zclient.core.controllers.tracking.attributes.RangedAttribute;
import com.waz.zclient.core.controllers.tracking.events.onboarding.KeptGeneratedUsernameEvent;
import com.waz.zclient.core.controllers.tracking.events.onboarding.OpenedUsernameSettingsEvent;
import com.waz.zclient.core.controllers.tracking.events.onboarding.SeenUsernameTakeOverScreenEvent;
import com.waz.zclient.newreg.fragments.FirstTimeAssignUsernameFragment;
import com.waz.zclient.pages.BaseFragment;
import com.waz.zclient.pages.main.backgroundmain.views.BackgroundFrameLayout;
import com.waz.zclient.pages.main.conversation.SingleImageFragment;
import com.waz.zclient.pages.main.conversation.SingleImageMessageFragment;
import com.waz.zclient.pages.main.conversation.SingleImageUserFragment;
import com.waz.zclient.pages.main.conversation.VideoPlayerFragment;
import com.waz.zclient.pages.main.conversationpager.ConversationPagerFragment;
import com.waz.zclient.pages.main.giphy.GiphySharingPreviewFragment;
import com.waz.zclient.pages.main.inappnotification.InAppNotificationFragment;
import com.waz.zclient.pages.main.onboarding.OnBoardingHintFragment;
import com.waz.zclient.pages.main.onboarding.OnBoardingHintType;
import com.waz.zclient.pages.main.profile.ZetaPreferencesActivity;
import com.waz.zclient.utils.ViewUtils;
import com.waz.zclient.views.menus.ConfirmationMenu;

public class MainPhoneFragment extends BaseFragment<MainPhoneFragment.Container> implements OnBackPressedListener,
                                                                                            ConversationPagerFragment.Container,
                                                                                            InAppNotificationFragment.Container,
                                                                                            OnBoardingHintFragment.Container,
                                                                                            OnboardingControllerObserver,
                                                                                            SingleImageObserver,
                                                                                            SingleImageFragment.Container,
                                                                                            GiphyObserver,
                                                                                            ConfirmationObserver,
                                                                                            AccentColorObserver,
                                                                                            UsernamesControllerObserver,
                                                                                            FirstTimeAssignUsernameFragment.Container,
                                                                                            NavigationControllerObserver {

    public static final String TAG = MainPhoneFragment.class.getName();
    private ConfirmationMenu confirmationMenu;
    private BackgroundFrameLayout backgroundLayout;

    ////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    //  LifeCycle
    //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);

        if (savedInstanceState == null) {
            getChildFragmentManager().beginTransaction().add(R.id.fl_fragment_main_in_app_notification,
                                                             InAppNotificationFragment.newInstance(),
                                                             InAppNotificationFragment.TAG).commit();
            getChildFragmentManager().beginTransaction().replace(R.id.fl_fragment_main_content,
                                                                 ConversationPagerFragment.newInstance(),
                                                                 ConversationPagerFragment.TAG).commit();
        }

        backgroundLayout = ViewUtils.getView(view, R.id.bl__background);
        confirmationMenu = ViewUtils.getView(view, R.id.cm__confirm_action_light);
        confirmationMenu.setVisibility(View.GONE);

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        getControllerFactory().getSingleImageController().addSingleImageObserver(this);
        getControllerFactory().getOnboardingController().addOnboardingControllerObserver(this);
        getControllerFactory().getGiphyController().addObserver(this);
        getControllerFactory().getConfirmationController().addConfirmationObserver(this);
        getControllerFactory().getAccentColorController().addAccentColorObserver(this);
        getControllerFactory().getNavigationController().addNavigationControllerObserver(this);

        getControllerFactory().getAccentColorController().addAccentColorObserver(backgroundLayout);
        getControllerFactory().getBackgroundController().addBackgroundObserver(backgroundLayout);

        OnBoardingHintFragment fragment = (OnBoardingHintFragment) getChildFragmentManager().findFragmentByTag(
            OnBoardingHintFragment.TAG);
        if (fragment != null) {
            getControllerFactory().getOnboardingController().setCurrentHintType(fragment.getOnBoardingHintType());
        }
    }

    @Override
    public void onStop() {
        getControllerFactory().getGiphyController().removeObserver(this);
        getControllerFactory().getSingleImageController().removeSingleImageObserver(this);
        getControllerFactory().getOnboardingController().removeOnboardingControllerObserver(this);
        getControllerFactory().getConfirmationController().removeConfirmationObserver(this);
        getControllerFactory().getAccentColorController().removeAccentColorObserver(this);
        getControllerFactory().getNavigationController().removeNavigationControllerObserver(this);
        getControllerFactory().getUsernameController().removeUsernamesObserver(this);

        getControllerFactory().getAccentColorController().removeAccentColorObserver(backgroundLayout);
        getControllerFactory().getBackgroundController().removeBackgroundObserver(backgroundLayout);
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        confirmationMenu = null;
        super.onDestroyView();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        getChildFragmentManager().findFragmentById(R.id.fl_fragment_main_content).onActivityResult(requestCode,
                                                                                                   resultCode,
                                                                                                   data);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    //  Notifications
    //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////


    @Override
    public boolean onBackPressed() {
        if (confirmationMenu.getVisibility() == View.VISIBLE) {
            confirmationMenu.animateToShow(false);
            return true;
        }

        // Clear any overlays
        dismissOnBoardingHint(OnBoardingHintType.NONE);

        if (getChildFragmentManager().getBackStackEntryCount() > 0) {
            Fragment topFragment = getChildFragmentManager().findFragmentByTag(getChildFragmentManager().getBackStackEntryAt(
                0).getName());
            if (topFragment instanceof SingleImageFragment) {
                return ((SingleImageFragment) topFragment).onBackPressed();
            } else if (topFragment instanceof VideoPlayerFragment) {
                return ((VideoPlayerFragment) topFragment).onBackPressed();
            } else if (topFragment instanceof GiphySharingPreviewFragment) {
                if (!((GiphySharingPreviewFragment) topFragment).onBackPressed()) {
                    getChildFragmentManager().popBackStackImmediate(GiphySharingPreviewFragment.TAG,
                                                                    FragmentManager.POP_BACK_STACK_INCLUSIVE);
                }
                return true;
            }

        }

        // Back press is first delivered to the notification fragment, and if it's not consumed there,
        // it's then delivered to the main content.

        Fragment fragment = getChildFragmentManager().findFragmentById(R.id.fl_fragment_main_in_app_notification);
        if (fragment instanceof OnBackPressedListener &&
            ((OnBackPressedListener) fragment).onBackPressed()) {
            return true;
        }

        fragment = getChildFragmentManager().findFragmentById(R.id.fl_fragment_main_content);
        if (fragment instanceof OnBackPressedListener &&
            ((OnBackPressedListener) fragment).onBackPressed()) {
            return true;
        }

        fragment = getChildFragmentManager().findFragmentById(R.id.fl__overlay_container);
        if (fragment instanceof OnBackPressedListener &&
            ((OnBackPressedListener) fragment).onBackPressed()) {
            return true;
        }

        return getChildFragmentManager().popBackStackImmediate();
    }

    @Override
    public void onOpenUrl(String url) {
        getContainer().onOpenUrl(url);
    }

    //////////////////////////////////////////////////////////////////////////////////////////
    //
    //  OnBoardingHintFragment
    //
    //////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void dismissOnBoardingHint(OnBoardingHintType requestedType) {
        getControllerFactory().getOnboardingController().hideOnboardingHint(requestedType);
    }

    @Override
    public void onShowOnboardingHint(final OnBoardingHintType hintType, int delayMilSec) {
        if (hintType == OnBoardingHintType.NONE) {
            return;
        }

        new Handler(Looper.myLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                if (getContainer() == null || !isResumed()) {
                    return;
                }

                // Additional check if hint types match. Some animations go through conversation list and might trigger pull down hint
                Page currentPage = getControllerFactory().getNavigationController().getCurrentPage();
                IConversation currentConversation = getStoreFactory().getConversationStore().getCurrentConversation();

                boolean currentConversationHasDraft = TextUtils.isEmpty(getStoreFactory().getDraftStore().getDraft(
                    getStoreFactory().getConversationStore().getCurrentConversation()));
                OnBoardingHintType currentHintType = getControllerFactory().getOnboardingController().getCurrentOnBoardingHint(
                    currentPage,
                    currentConversation,
                    currentConversationHasDraft);
                if (hintType != currentHintType) {
                    return;
                }

                getChildFragmentManager().popBackStackImmediate(OnBoardingHintFragment.TAG,
                                                                FragmentManager.POP_BACK_STACK_INCLUSIVE);
                getChildFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(R.anim.fade_in,
                                         R.anim.slide_out_to_top,
                                         R.anim.slide_in_from_bottom,
                                         R.anim.fade_out)
                    .add(R.id.fl_fragment_main_onboarding,
                         OnBoardingHintFragment.newInstance(hintType),
                         OnBoardingHintFragment.TAG)
                    .addToBackStack(OnBoardingHintFragment.TAG)
                    .commit();
            }
        }, delayMilSec);
    }

    @Override
    public void onHideOnboardingHint(OnBoardingHintType type) {
        getChildFragmentManager().popBackStackImmediate(OnBoardingHintFragment.TAG,
                                                        FragmentManager.POP_BACK_STACK_INCLUSIVE);
    }

    @Override
    public void onShowSingleImage(Message message) {
        getChildFragmentManager().beginTransaction()
                                 .add(R.id.fl__overlay_container,
                                      SingleImageMessageFragment.newInstance(message),
                                      SingleImageMessageFragment.TAG)
                                 .addToBackStack(SingleImageMessageFragment.TAG)
                                 .commit();
        getControllerFactory().getNavigationController().setRightPage(Page.SINGLE_MESSAGE, TAG);
        getControllerFactory().getTrackingController().updateSessionAggregates(RangedAttribute.IMAGE_CONTENT_CLICKS);
    }

    @Override
    public void onShowUserImage(User user) {
        getChildFragmentManager().beginTransaction()
                                 .add(R.id.fl__overlay_container,
                                      SingleImageUserFragment.newInstance(user),
                                      SingleImageUserFragment.TAG)
                                 .addToBackStack(SingleImageUserFragment.TAG)
                                 .commit();
        getControllerFactory().getNavigationController().setRightPage(Page.SINGLE_MESSAGE, TAG);
        getControllerFactory().getTrackingController().updateSessionAggregates(RangedAttribute.IMAGE_CONTENT_CLICKS);
    }

    @Override
    public void onHideSingleImage() {

    }

    @Override
    public void updateSingleImageReferences() {

    }

    @Override
    public void onShowVideo(Uri uri) {
        getChildFragmentManager().beginTransaction()
                                 .add(R.id.fl__overlay_container,
                                      VideoPlayerFragment.newInstance(uri),
                                      VideoPlayerFragment.TAG)
                                 .addToBackStack(VideoPlayerFragment.TAG)
                                 .commit();
        getControllerFactory().getNavigationController().setRightPage(Page.SINGLE_MESSAGE, TAG);
    }

    @Override
    public void onHideVideo() {
        getChildFragmentManager().popBackStackImmediate(VideoPlayerFragment.TAG,
                                                        FragmentManager.POP_BACK_STACK_INCLUSIVE);
    }

    @Override
    public void onSearch(String keyword) {
        getChildFragmentManager().beginTransaction()
                                 .add(R.id.fl__overlay_container,
                                      GiphySharingPreviewFragment.newInstance(keyword),
                                      GiphySharingPreviewFragment.TAG)
                                 .addToBackStack(GiphySharingPreviewFragment.TAG)
                                 .commit();
    }

    @Override
    public void onRandomSearch() {
        getChildFragmentManager().beginTransaction()
                                 .add(R.id.fl__overlay_container,
                                      GiphySharingPreviewFragment.newInstance(),
                                      GiphySharingPreviewFragment.TAG)
                                 .addToBackStack(GiphySharingPreviewFragment.TAG)
                                 .commit();
    }

    @Override
    public void onTrendingSearch() {
        getChildFragmentManager().beginTransaction()
                                 .add(R.id.fl__overlay_container,
                                      GiphySharingPreviewFragment.newInstance(),
                                      GiphySharingPreviewFragment.TAG)
                                 .addToBackStack(GiphySharingPreviewFragment.TAG)
                                 .commit();
    }

    @Override
    public void onCloseGiphy() {
        getChildFragmentManager().popBackStackImmediate(GiphySharingPreviewFragment.TAG,
                                                        FragmentManager.POP_BACK_STACK_INCLUSIVE);
    }

    @Override
    public void onCancelGiphy() {
        getChildFragmentManager().popBackStackImmediate(GiphySharingPreviewFragment.TAG,
                                                        FragmentManager.POP_BACK_STACK_INCLUSIVE);
    }

    //////////////////////////////////////////////////////////////////////////////////////////
    //
    //  ConfirmationObserver
    //
    //////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onRequestConfirmation(ConfirmationRequest confirmationRequest, @IConfirmationController.ConfirmationMenuRequester int requester) {
        confirmationMenu.onRequestConfirmation(confirmationRequest);
    }

    //////////////////////////////////////////////////////////////////////////////////////////
    //
    //  AccentColorObserver
    //
    //////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onAccentColorHasChanged(Object sender, int color) {
        if (getView() == null) {
            return;
        }
        confirmationMenu.setButtonColor(color);
    }

    //////////////////////////////////////////////////////////////////////////////////////////
    //
    //  UsernamesObserver
    //
    //////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onValidUsernameGenerated(String name, String generatedUsername) {
        openUsernameAssignTakeOver(name, generatedUsername);
    }

    @Override
    public void onUsernameAttemptsExhausted(String name) {
        openUsernameAssignTakeOver(name, "");
    }

    private void openUsernameAssignTakeOver(String name, String username) {
        if (getControllerFactory().getNavigationController().getCurrentLeftPage() != Page.CONVERSATION_LIST ||
            getChildFragmentManager().findFragmentByTag(FirstTimeAssignUsernameFragment.TAG) != null) {
            return;
        }
        getChildFragmentManager().beginTransaction()
            .setCustomAnimations(R.anim.fade_in,
                R.anim.fade_out,
                R.anim.fade_in,
                R.anim.fade_out)
            .add(R.id.fl__overlay_container,
                 FirstTimeAssignUsernameFragment.newInstance(name, username),
                 FirstTimeAssignUsernameFragment.TAG)
            .addToBackStack(FirstTimeAssignUsernameFragment.TAG)
            .commit();

        getControllerFactory().getTrackingController().tagEvent(new SeenUsernameTakeOverScreenEvent());
    }

    //////////////////////////////////////////////////////////////////////////////////////////
    //
    //  FirstTimeAssignUsername.Container
    //
    //////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onChooseUsernameChosen() {
        getChildFragmentManager().popBackStackImmediate(FirstTimeAssignUsernameFragment.TAG,
                                                        FragmentManager.POP_BACK_STACK_INCLUSIVE);
        getControllerFactory().getTrackingController().tagEvent(new OpenedUsernameSettingsEvent());
        startActivity(ZetaPreferencesActivity.getUsernameEditPreferencesIntent(getActivity()));
    }

    @Override
    public void onKeepUsernameChosen(String username) {
        getChildFragmentManager().popBackStackImmediate(FirstTimeAssignUsernameFragment.TAG,
                                                        FragmentManager.POP_BACK_STACK_INCLUSIVE);
        getStoreFactory().getZMessagingApiStore().getApi().getSelf().setUsername(username, new CredentialsUpdateListener() {
            @Override
            public void onUpdated() {
                getControllerFactory().getTrackingController().tagEvent(new KeptGeneratedUsernameEvent(true));
            }
            @Override
            public void onUpdateFailed(int code, String message, String label) {
                Toast.makeText(getActivity(), getString(R.string.username__set__toast_error), Toast.LENGTH_SHORT).show();
                getControllerFactory().getUsernameController().logout();
                getControllerFactory().getUsernameController().setUser(getStoreFactory().getZMessagingApiStore().getApi().getSelf());
                getControllerFactory().getTrackingController().tagEvent(new KeptGeneratedUsernameEvent(false));
            }
        });
    }

    //////////////////////////////////////////////////////////////////////////////////////////
    //
    //  NavigationControllerObserver
    //
    //////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onPageVisible(Page page) {
        if (page == Page.CONVERSATION_LIST) {
            getControllerFactory().getUsernameController().addUsernamesObserver(this);
        } else {
            getControllerFactory().getUsernameController().removeUsernamesObserver(this);
        }
    }

    @Override
    public void onPageStateHasChanged(Page page) {

    }

    public interface Container {
        void onOpenUrl(String url);
    }
}

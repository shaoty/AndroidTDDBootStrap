package com.github.piasy.model.users.dao;

import com.github.piasy.base.model.provider.GsonProviderExposure;
import com.github.piasy.base.test.BaseThreeTenBPTest;
import com.github.piasy.base.test.MockProvider;
import com.github.piasy.model.errors.ApiError;
import com.github.piasy.model.users.GithubUser;
import com.github.piasy.model.users.GithubApi;
import com.github.piasy.model.users.GithubUserSearchResult;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import rx.Observable;
import rx.Subscriber;
import rx.observers.TestSubscriber;

import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;

/**
 * Created by Piasy{github.com/Piasy} on 15/8/12.
 */
public class GithubUserDaoImplTest extends BaseThreeTenBPTest {

    private DbUserDelegate mDbUserDelegate;
    private GithubApi mGithubApi;
    private GithubUserDao mGithubUserDao;

    private GithubUserSearchResult mEmptyResult;

    @Before
    public void setUp() {
        initThreeTenBP();
        final Gson gson = GsonProviderExposure.exposeGson();
        mEmptyResult = gson.fromJson(MockProvider.provideEmptyGithubSearchResult(),
                new TypeToken<GithubUserSearchResult>() {}.getType());

        mDbUserDelegate = mock(DbUserDelegate.class);
        mGithubApi = mock(GithubApi.class);

        mGithubUserDao = new GithubUserDaoImpl(mDbUserDelegate, mGithubApi);
    }

    @Test
    public void testSearchUserSuccess() {
        // given
        willReturn(Observable.create(new Observable.OnSubscribe<GithubUserSearchResult>() {
            @Override
            public void call(final Subscriber<? super GithubUserSearchResult> subscriber) {
                subscriber.onNext(mEmptyResult);
                subscriber.onCompleted();
            }
        })).given(mGithubApi).searchGithubUsers(anyString(), anyString(), anyString());

        // when
        final TestSubscriber<List<GithubUser>> subscriber = new TestSubscriber<>();
        mGithubUserDao.searchUser("Piasy").subscribe(subscriber);
        subscriber.awaitTerminalEvent();

        // then
        then(mDbUserDelegate).should(timeout(100)).putAllGithubUser(anyListOf(GithubUser.class));
        verifyNoMoreInteractions(mDbUserDelegate);
        subscriber.assertNoErrors();

        then(mGithubApi).should(timeout(100).only())
                .searchGithubUsers(anyString(), anyString(), anyString());
    }

    @Test
    public void testSearchUserApiError() {
        // given
        willReturn(Observable.create(new Observable.OnSubscribe<GithubUserSearchResult>() {
            @Override
            public void call(final Subscriber<? super GithubUserSearchResult> subscriber) {
                ApiError apiError = new ApiError();
                subscriber.onError(apiError);
            }
        })).given(mGithubApi).searchGithubUsers(anyString(), anyString(), anyString());

        // when
        final TestSubscriber<List<GithubUser>> subscriber = new TestSubscriber<>();
        mGithubUserDao.searchUser("Piasy").subscribe(subscriber);
        subscriber.awaitTerminalEvent();

        // then
        verifyZeroInteractions(mDbUserDelegate);
        subscriber.assertNoValues();
        subscriber.assertError(ApiError.class);

        then(mGithubApi).should(timeout(100).only())
                .searchGithubUsers(anyString(), anyString(), anyString());
    }
}

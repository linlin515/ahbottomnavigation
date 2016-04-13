package com.aurelhubert.ahbottomnavigation;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPropertyAnimatorCompat;
import android.support.v4.view.ViewPropertyAnimatorUpdateListener;
import android.support.v4.view.animation.LinearOutSlowInInterpolator;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;

/**
 *
 */
public class AHBottomNavigationBehavior<V extends View> extends VerticalScrollingBehavior<V> {

	private static final Interpolator INTERPOLATOR = new LinearOutSlowInInterpolator();
	private static final int ANIM_DURATION = 300;

	private int mTabLayoutId;
	private boolean hidden = false;
	private ViewPropertyAnimatorCompat mTranslationAnimator;
	private TabLayout mTabLayout;
	private Snackbar.SnackbarLayout snackbarLayout;
	private FloatingActionButton floatingActionButton;
	private int mSnackbarHeight = -1;
	private boolean fabBottomMarginInitialized = false;
	private float targetOffset = 0, fabTargetOffset = 0, fabDefaultBottomMargin = 0, snackBarY = 0;

	public AHBottomNavigationBehavior() {
		super();
	}

	public AHBottomNavigationBehavior(Context context, AttributeSet attrs) {
		super(context, attrs);
		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.AHBottomNavigationBehavior_Params);
		mTabLayoutId = a.getResourceId(R.styleable.AHBottomNavigationBehavior_Params_tabLayoutId, View.NO_ID);
		a.recycle();
	}

	@Override
	public boolean onLayoutChild(CoordinatorLayout parent, V child, int layoutDirection) {
		boolean layoutChild = super.onLayoutChild(parent, child, layoutDirection);
		if (mTabLayout == null && mTabLayoutId != View.NO_ID) {
			mTabLayout = findTabLayout(child);
		}
		return layoutChild;
	}

	private TabLayout findTabLayout(View child) {
		if (mTabLayoutId == 0) return null;
		return (TabLayout) child.findViewById(mTabLayoutId);
	}

	@Override
	public boolean onDependentViewChanged(CoordinatorLayout parent, V child, View dependency) {
		return super.onDependentViewChanged(parent, child, dependency);
	}

	@Override
	public void onDependentViewRemoved(CoordinatorLayout parent, V child, View dependency) {
		super.onDependentViewRemoved(parent, child, dependency);
	}

	@Override
	public boolean layoutDependsOn(CoordinatorLayout parent, V child, View dependency) {
		updateSnackbar(child, dependency);
		updateFloatingActionButton(dependency);
		return super.layoutDependsOn(parent, child, dependency);
	}

	@Override
	public void onNestedVerticalOverScroll(CoordinatorLayout coordinatorLayout, V child, @ScrollDirection int direction, int currentOverScroll, int totalOverScroll) {
	}

	@Override
	public void onDirectionNestedPreScroll(CoordinatorLayout coordinatorLayout, V child, View target, int dx, int dy, int[] consumed, @ScrollDirection int scrollDirection) {
		handleDirection(child, scrollDirection);
	}


	private void handleDirection(V child, int scrollDirection) {
		if (scrollDirection == ScrollDirection.SCROLL_DIRECTION_DOWN && hidden) {
			hidden = false;
			animateOffset(child, 0);
		} else if (scrollDirection == ScrollDirection.SCROLL_DIRECTION_UP && !hidden) {
			hidden = true;
			animateOffset(child, child.getHeight());
		}
	}

	@Override
	protected boolean onNestedDirectionFling(CoordinatorLayout coordinatorLayout, V child, View target, float velocityX, float velocityY, @ScrollDirection int scrollDirection) {
		handleDirection(child, scrollDirection);
		return true;
	}

	private void animateOffset(final V child, final int offset) {
		ensureOrCancelAnimator(child);
		mTranslationAnimator.translationY(offset).start();
	}

	private void ensureOrCancelAnimator(V child) {
		if (mTranslationAnimator == null) {
			mTranslationAnimator = ViewCompat.animate(child);
			mTranslationAnimator.setDuration(ANIM_DURATION);
			mTranslationAnimator.setUpdateListener(new ViewPropertyAnimatorUpdateListener() {
				@Override
				public void onAnimationUpdate(View view) {
					// Animate snackbar
					if (snackbarLayout != null && snackbarLayout.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
						targetOffset = view.getMeasuredHeight() - view.getTranslationY();
						ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) snackbarLayout.getLayoutParams();
						p.setMargins(p.leftMargin, p.topMargin, p.rightMargin, (int) targetOffset);
						snackbarLayout.requestLayout();
					}
					// Animate Floating Action Button
					if (floatingActionButton != null && floatingActionButton.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
						ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) floatingActionButton.getLayoutParams();
						fabTargetOffset = fabDefaultBottomMargin - view.getTranslationY() + snackBarY;
						p.setMargins(p.leftMargin, p.topMargin, p.rightMargin, (int) fabTargetOffset);
						floatingActionButton.requestLayout();
					}
				}
			});
			mTranslationAnimator.setInterpolator(INTERPOLATOR);
		} else {
			mTranslationAnimator.cancel();
		}
	}


	public static <V extends View> AHBottomNavigationBehavior<V> from(V view) {
		ViewGroup.LayoutParams params = view.getLayoutParams();
		if (!(params instanceof CoordinatorLayout.LayoutParams)) {
			throw new IllegalArgumentException("The view is not a child of CoordinatorLayout");
		}
		CoordinatorLayout.Behavior behavior = ((CoordinatorLayout.LayoutParams) params)
				.getBehavior();
		if (!(behavior instanceof AHBottomNavigationBehavior)) {
			throw new IllegalArgumentException(
					"The view is not associated with AHBottomNavigationBehavior");
		}
		return (AHBottomNavigationBehavior<V>) behavior;
	}

	public void setTabLayoutId(int tabId) {
		this.mTabLayoutId = tabId;
	}

	public void resetOffset(V view) {
		animateOffset(view, 0);
	}

	/**
	 * Update Snackbar bottom margin
	 */
	public void updateSnackbar(final View child, View dependency) {

		if (dependency != null && dependency instanceof Snackbar.SnackbarLayout) {

			snackbarLayout = (Snackbar.SnackbarLayout) dependency;
			snackbarLayout.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
				@Override
				public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
					snackBarY = bottom - v.getY();
					if (floatingActionButton != null &&
							floatingActionButton.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
						ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) floatingActionButton.getLayoutParams();
						fabTargetOffset = fabDefaultBottomMargin - child.getTranslationY() + snackBarY;
						p.setMargins(p.leftMargin, p.topMargin, p.rightMargin, (int) fabTargetOffset);
						floatingActionButton.requestLayout();
					}
				}
			});

			if (mSnackbarHeight == -1) {
				mSnackbarHeight = dependency.getHeight();
			}

			int targetMargin = (int) (child.getMeasuredHeight() - child.getTranslationY());
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
				child.bringToFront();
			}

			if (dependency.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
				ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) dependency.getLayoutParams();
				p.setMargins(p.leftMargin, p.topMargin, p.rightMargin, targetMargin);
				dependency.requestLayout();
			}
		}
	}

	/**
	 * Update floating action button bottom margin
	 */
	public void updateFloatingActionButton(View dependency) {
		if (dependency != null && dependency instanceof  FloatingActionButton) {
			floatingActionButton = (FloatingActionButton) dependency;
			if (!fabBottomMarginInitialized && dependency.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
				fabBottomMarginInitialized = true;
				ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) dependency.getLayoutParams();
				fabDefaultBottomMargin = p.bottomMargin;
			}
		}
	}
}
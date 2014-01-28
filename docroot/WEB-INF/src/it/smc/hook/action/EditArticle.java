
package it.smc.hook.action;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletConfig;
import javax.portlet.PortletRequest;
import javax.servlet.http.HttpServletResponse;

import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.servlet.ServletResponseUtil;
import com.liferay.portal.kernel.struts.BaseStrutsPortletAction;
import com.liferay.portal.kernel.util.CalendarFactoryUtil;
import com.liferay.portal.kernel.util.CalendarUtil;
import com.liferay.portal.kernel.util.ContentTypes;
import com.liferay.portal.kernel.util.LocalizationUtil;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.kernel.workflow.WorkflowConstants;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portal.service.ServiceContextFactory;
import com.liferay.portal.theme.ThemeDisplay;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portlet.journal.model.JournalArticle;
import com.liferay.portlet.journal.service.JournalArticleServiceUtil;

public class EditArticle extends BaseStrutsPortletAction {

	@Override
	public void processAction(
		PortletConfig portletConfig, ActionRequest actionRequest,
		ActionResponse actionResponse)
		throws Exception {

		try {

			ThemeDisplay themeDisplay =
				(ThemeDisplay) actionRequest.getAttribute(WebKeys.THEME_DISPLAY);

			ServiceContext serviceContext = ServiceContextFactory.getInstance(
				JournalArticle.class.getName(), actionRequest);

			String articleId = ParamUtil.get(actionRequest, "articleId", "");
			long groupId = ParamUtil.getLong(actionRequest, "groupId");
			double version = ParamUtil.getDouble(actionRequest, "version");
			String content = ParamUtil.getString(actionRequest, "content");

			Locale locale = themeDisplay.getLocale();
			String currentLanguageId = themeDisplay.getLanguageId();

			_log.info(articleId);

			// Update article

			JournalArticle curArticle =
				JournalArticleServiceUtil.getArticle(
					groupId, articleId, version);

			if (!curArticle.isTemplateDriven()) {

				String curContent = curArticle.getContent();

				String defaultLanguageId =
					LocalizationUtil.getDefaultLanguageId(curContent);

				content = LocalizationUtil.updateLocalization(
					curContent, "static-content", content,
					currentLanguageId, defaultLanguageId, true,
					true);

				JournalArticle article = JournalArticleServiceUtil.getArticle(
					groupId, articleId, version);

				serviceContext.setAttribute(
					"defaultLanguageId", defaultLanguageId);
				serviceContext.setWorkflowAction(WorkflowConstants.ACTION_PUBLISH);

				Calendar displayD = CalendarFactoryUtil.getCalendar();
				displayD.setTime(article.getDisplayDate());

				int displayDateMonth = displayD.get(Calendar.MONTH);
				int displayDateDay = displayD.get(Calendar.DAY_OF_MONTH);
				int displayDateYear = displayD.get(Calendar.YEAR);
				int displayDateHour = displayD.get(Calendar.HOUR_OF_DAY);
				int displayDateMinute = displayD.get(Calendar.MINUTE);

				Calendar expD = CalendarFactoryUtil.getCalendar();

				if (article.getExpirationDate() != null) {
					expD.setTime(article.getExpirationDate());
				}

				int expirationDateMonth = expD.get(Calendar.MONTH);
				int expirationDateDay = expD.get(Calendar.DAY_OF_MONTH);
				int expirationDateYear = expD.get(Calendar.YEAR);
				int expirationDateHour = expD.get(Calendar.HOUR_OF_DAY);
				int expirationDateMinute = expD.get(Calendar.MINUTE);

				Calendar revD = CalendarFactoryUtil.getCalendar();

				if (article.getReviewDate() != null) {
					revD.setTime(article.getReviewDate());
				}

				int reviewDateDay = revD.get(Calendar.MONTH);
				int reviewDateMonth = revD.get(Calendar.DAY_OF_MONTH);
				int reviewDateYear = revD.get(Calendar.YEAR);
				int reviewDateHour = revD.get(Calendar.HOUR_OF_DAY);
				int reviewDateMinute = revD.get(Calendar.MINUTE);

				boolean neverExpire =  (article.getExpirationDate() == null);
				boolean neverReview = (article.getReviewDate() == null);

				File smallFile = null;
				Map<String, byte[]> images = null;
				String articleURL = null;

				article =
					JournalArticleServiceUtil.updateArticle(
						groupId, article.getFolderId(), articleId, version,
						article.getTitleMap(), article.getDescriptionMap(),
						content, article.getType(), article.getStructureId(),
						article.getTemplateId(), article.getLayoutUuid(),
						displayDateMonth, displayDateDay, displayDateYear,
						displayDateHour, displayDateMinute,
						expirationDateMonth, expirationDateDay,
						expirationDateYear, expirationDateHour,
						expirationDateMinute, neverExpire, reviewDateMonth,
						reviewDateDay, reviewDateYear, reviewDateHour,
						reviewDateMinute, neverReview, article.getIndexable(),
						article.getSmallImage(), article.getSmallImageURL(),
						smallFile, images, articleURL, serviceContext);
			}

			JSONObject jsonObject = JSONFactoryUtil.createJSONObject();

			jsonObject.put("success", Boolean.TRUE);

			writeJSON(actionRequest, actionResponse, jsonObject);
		}
		catch (Exception e) {

			_log.error(e);

			JSONObject jsonObject = JSONFactoryUtil.createJSONObject();

			jsonObject.put("success", Boolean.FALSE);

			jsonObject.putException(e);

			writeJSON(actionRequest, actionResponse, jsonObject);
		}
	}

	protected void writeJSON(
		PortletRequest portletRequest, ActionResponse actionResponse,
		Object json)
		throws IOException {

		HttpServletResponse response =
			PortalUtil.getHttpServletResponse(actionResponse);

		response.setContentType(ContentTypes.APPLICATION_JSON);

		ServletResponseUtil.write(response, json.toString());

		response.flushBuffer();

		setForward(portletRequest, COMMON_NULL);
	}

	protected void setForward(PortletRequest portletRequest, String forward) {

		portletRequest.setAttribute(getForwardKey(portletRequest), forward);
	}

	public static String getForwardKey(PortletRequest portletRequest) {

		String portletId =
			(String) portletRequest.getAttribute(WebKeys.PORTLET_ID);

		String portletNamespace = PortalUtil.getPortletNamespace(portletId);

		return portletNamespace.concat(PORTLET_STRUTS_FORWARD);
	}

	public static final String PORTLET_STRUTS_FORWARD =
		"PORTLET_STRUTS_FORWARD";

	public static final String COMMON_NULL = "/common/null.jsp";

	private static Log _log =
		LogFactoryUtil.getLog(EditArticle.class.getName());
}

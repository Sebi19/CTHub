/* eslint-disable */
/* tslint:disable */
// @ts-nocheck
/*
 * ---------------------------------------------------------------
 * ## THIS FILE WAS GENERATED VIA SWAGGER-TYPESCRIPT-API        ##
 * ##                                                           ##
 * ## AUTHOR: acacode                                           ##
 * ## SOURCE: https://github.com/acacode/swagger-typescript-api ##
 * ---------------------------------------------------------------
 */

export enum AuthMethod {
  PASSKEY = "PASSKEY",
  PASSWORD = "PASSWORD",
  TOTP = "TOTP",
  EMAIL = "EMAIL",
  PUSH = "PUSH",
}

export enum MailOutboxStatus {
  PENDING = "PENDING",
  SENDING = "SENDING",
  SENT = "SENT",
  FAILED = "FAILED",
  CANCELLED = "CANCELLED",
}

export enum ChallengeViewKind {
  SAME_BROWSER = "SAME_BROWSER",
  CROSS = "CROSS",
  INVALID = "INVALID",
}

export enum SearchResultTypeDto {
  SEASON_TEAM = "SEASON_TEAM",
  TEAM_PROFILE = "TEAM_PROFILE",
  COMPETITION = "COMPETITION",
  SEASON = "SEASON",
}

export enum CompetitionType {
  REGIONAL = "REGIONAL",
  QUALIFICATION = "QUALIFICATION",
  FINAL = "FINAL",
}

export enum CompetitionAwardCategoryDto {
  CHAMPION = "CHAMPION",
  RESEARCH = "RESEARCH",
  CORE_VALUES = "CORE_VALUES",
  ROBOT_DESIGN = "ROBOT_DESIGN",
  COACHING = "COACHING",
  ROBOT_GAME = "ROBOT_GAME",
}

export enum AuthNextStep {
  PASSKEY = "PASSKEY",
  PASSWORD = "PASSWORD",
  CHALLENGE = "CHALLENGE",
}

export enum ChallengePickResult {
  APPROVED = "APPROVED",
  REJECTED = "REJECTED",
}

export enum LoginResultStatus {
  SIGNED_IN = "SIGNED_IN",
  TOTP_REQUIRED = "TOTP_REQUIRED",
}

export enum LoginAttemptState {
  WAITING = "WAITING",
  APPROVED = "APPROVED",
  COMPLETED = "COMPLETED",
  REJECTED = "REJECTED",
  CANCELLED = "CANCELLED",
  CLAIMED_ELSEWHERE = "CLAIMED_ELSEWHERE",
  EXPIRED = "EXPIRED",
}

export enum LoginAttemptPurpose {
  LOGIN = "LOGIN",
  STEP_UP = "STEP_UP",
}

export enum NotificationTypeDto {
  WELCOME = "WELCOME",
  ANNOUNCEMENT = "ANNOUNCEMENT",
}

export enum NotificationChannelDto {
  EMAIL = "EMAIL",
  PUSH = "PUSH",
}

export interface PushSubscriptionRequestDto {
  /**
   * @minLength 0
   * @maxLength 2048
   */
  endpoint: string;
  /**
   * @minLength 0
   * @maxLength 128
   */
  p256dh: string;
  /**
   * @minLength 0
   * @maxLength 64
   */
  auth: string;
  installed: boolean;
  /**
   * @minLength 0
   * @maxLength 2048
   */
  previousEndpoint?: string;
  renewOnly?: boolean;
}

export interface PushSubscriptionDto {
  /** @format int64 */
  id: number;
  userAgent?: string;
  installed: boolean;
  /** @format date-time */
  createdAt: string;
  /** @format date-time */
  lastSeenAt: string;
}

export interface NotificationPreferenceDto {
  type: NotificationTypeDto;
  channel: NotificationChannelDto;
  enabled: boolean;
}

export interface SetPasswordRequestDto {
  /**
   * @minLength 0
   * @maxLength 256
   */
  password: string;
}

export interface UserDto {
  /** @format int64 */
  id: number;
  email: string;
  firstName?: string;
  lastName?: string;
  role?: string;
  preferredLocale?: string;
  profileComplete?: boolean;
  passwordSet?: boolean;
}

export interface NotificationDto {
  /** @format int64 */
  id: number;
  type: NotificationTypeDto;
  params: Record<string, string>;
  link?: string;
  /** @format date-time */
  createdAt: string;
  /** @format date-time */
  readAt?: string;
}

export interface MarkAllReadRequestDto {
  /** @format int64 */
  upToId: number;
}

export interface UnreadCountDto {
  /** @format int64 */
  count?: number;
}

export interface PushTestResultDto {
  /** @format int32 */
  devices: number;
  /** @format int32 */
  delivered: number;
}

export interface PushUnregisterRequestDto {
  /**
   * @minLength 0
   * @maxLength 2048
   */
  endpoint: string;
}

export interface TotpCodeRequestDto {
  /**
   * @minLength 0
   * @maxLength 16
   */
  code: string;
}

export interface PasskeyResponseDto {
  /**
   * @minLength 0
   * @maxLength 32768
   */
  credentialJson: string;
}

export interface PasskeyAuthenticationOptionsDto {
  challenge: string;
  rpId: string;
  /** @format int64 */
  timeout?: number;
  userVerification: string;
  allowCredentials: PasskeyDescriptorDto[];
}

export interface PasskeyDescriptorDto {
  id: string;
  type: string;
  transports?: string[];
}

export interface PushEndpointRequestDto {
  /**
   * @minLength 0
   * @maxLength 2048
   */
  pushEndpoint?: string;
}

export interface LoginAttemptStateDto {
  state: LoginAttemptState;
  purpose: LoginAttemptPurpose;
  /** @format int32 */
  challengeNumber?: number;
  /** @format date-time */
  expiresAt: string;
  openedElsewhere?: boolean;
}

export interface PasswordLoginRequestDto {
  /**
   * @format email
   * @minLength 0
   * @maxLength 255
   */
  email: string;
  /**
   * @minLength 0
   * @maxLength 256
   */
  password: string;
}

export interface LoginResultDto {
  status: LoginResultStatus;
  user?: UserDto;
}

export interface LinkTokenRequestDto {
  /**
   * @minLength 0
   * @maxLength 128
   */
  token: string;
}

export interface AttemptCompletionDto {
  purpose: LoginAttemptPurpose;
  user: UserDto;
}

export interface LinkApproveRequestDto {
  /**
   * @minLength 0
   * @maxLength 128
   */
  token: string;
  /**
   * @format int32
   * @min 0
   * @max 99
   */
  number?: number;
}

export interface ChallengePickResultDto {
  result: ChallengePickResult;
}

export interface IdentifyRequestDto {
  /**
   * @format email
   * @minLength 0
   * @maxLength 255
   */
  email: string;
  /**
   * @minLength 0
   * @maxLength 2048
   */
  pushEndpoint?: string;
  /**
   * @minLength 0
   * @maxLength 35
   */
  locale?: string;
}

export interface IdentifyResponseDto {
  next: AuthNextStep;
  passwordAvailable?: boolean;
  attempt?: LoginAttemptStateDto;
}

export interface CompleteProfileRequestDto {
  /**
   * @minLength 0
   * @maxLength 100
   */
  firstName: string;
  /**
   * @minLength 0
   * @maxLength 100
   */
  lastName: string;
}

export interface StartAttemptRequestDto {
  /**
   * @format email
   * @minLength 0
   * @maxLength 255
   */
  email: string;
  /**
   * @minLength 0
   * @maxLength 2048
   */
  pushEndpoint?: string;
  /**
   * @minLength 0
   * @maxLength 35
   */
  locale?: string;
}

export interface ApprovalPickRequestDto {
  /**
   * @format int32
   * @min 0
   * @max 99
   */
  number?: number;
}

export interface AnnouncementRequestDto {
  /**
   * @minLength 0
   * @maxLength 200
   */
  title: string;
  /**
   * @minLength 0
   * @maxLength 2000
   */
  body: string;
  /**
   * @minLength 0
   * @maxLength 2048
   */
  link?: string;
  /** @format int64 */
  userId?: number;
}

export interface AnnouncementResultDto {
  /** @format int32 */
  recipients?: number;
}

export interface TestMailRequestDto {
  /**
   * @format email
   * @minLength 1
   */
  email: string;
}

export interface MailOutboxEntryDto {
  /** @format int64 */
  id?: number;
  recipientEmail?: string;
  subject?: string;
  template?: string;
  category?: string;
  status?: string;
  /** @format int32 */
  attempts?: number;
  lastError?: string;
  /** @format date-time */
  nextAttemptAt?: string;
  /** @format date-time */
  createdAt?: string;
  /** @format date-time */
  sentAt?: string;
}

export interface TotpEnrollmentDto {
  secret: string;
  otpauthUri: string;
}

export interface PasskeyDto {
  /** @format int64 */
  id: number;
  name: string;
  /** @format date-time */
  createdAt: string;
  /** @format date-time */
  lastUsedAt?: string;
  synced?: boolean;
}

export interface PasskeyAuthenticatorSelectionDto {
  residentKey: string;
  requireResidentKey?: boolean;
  userVerification: string;
}

export interface PasskeyCredentialParamDto {
  type: string;
  /** @format int32 */
  alg?: number;
}

export interface PasskeyRegistrationOptionsDto {
  rp: PasskeyRpDto;
  user: PasskeyUserDto;
  challenge: string;
  pubKeyCredParams: PasskeyCredentialParamDto[];
  /** @format int64 */
  timeout?: number;
  excludeCredentials: PasskeyDescriptorDto[];
  authenticatorSelection: PasskeyAuthenticatorSelectionDto;
  attestation: string;
}

export interface PasskeyRpDto {
  id: string;
  name: string;
}

export interface PasskeyUserDto {
  id: string;
  name: string;
  displayName: string;
}

export interface EmailChangeRequestDto {
  /**
   * @format email
   * @minLength 0
   * @maxLength 255
   */
  newEmail: string;
}

export interface RenamePasskeyRequestDto {
  /**
   * @minLength 0
   * @maxLength 64
   */
  name: string;
}

export interface CompetitionNominationDto {
  /** @format int64 */
  teamId: number;
  category: CompetitionAwardCategoryDto;
  winner?: boolean;
}

export interface CompetitionPlaceDto {
  /** @format int64 */
  teamId: number;
  /** @format int32 */
  place: number;
  advancing: boolean;
}

export interface CompetitionRobotGameEntryDto {
  /** @format int64 */
  teamId: number;
  /** @format int32 */
  rank: number;
  /** @format int32 */
  pr1: number;
  /** @format int32 */
  pr2: number;
  /** @format int32 */
  pr3: number;
  /** @format int32 */
  bestPr: number;
  /** @format int32 */
  r16?: number;
  /** @format int32 */
  qf?: number;
  /** @format int32 */
  sf?: number;
  /** @format int32 */
  f1?: number;
  /** @format int32 */
  f2?: number;
  /** @format int32 */
  prelimRank: number;
}

export interface CompetitionShortInfoDto {
  /** @format int64 */
  id: number;
  season: SeasonDto;
  name: string;
  urlPart: string;
  slug?: string;
  type: CompetitionType;
  active: boolean;
  resultsAvailable: boolean;
  country?: string;
  /** @format date */
  date?: string;
  /** @format date */
  endDate?: string;
  /** @format int32 */
  registeredTeamCount?: number;
  /** @format int32 */
  maxTeamCount?: number;
  newApiCompetition: boolean;
}

export interface LinkDto {
  label?: string;
  url: string;
}

export interface SeasonDto {
  id: string;
  name: string;
  /** @format int32 */
  startYear: number;
  active: boolean;
  /** @format int32 */
  maxPoints?: number;
}

export interface SeasonTeamDetailsDto {
  /** @format int64 */
  id: number;
  season: SeasonDto;
  active: boolean;
  fllId: string;
  name: string;
  institution?: string;
  city?: string;
  country?: string;
  links: LinkDto[];
  seasonTeamProfile?: SeasonTeamProfileDto;
  competitionRecords: TeamCompetitionRecordDto[];
}

export interface SeasonTeamProfileDto {
  profile: TeamProfileDto;
  seasonAvatarUrl?: string;
}

export interface TeamCompetitionRecordDto {
  competition: CompetitionShortInfoDto;
  place?: CompetitionPlaceDto;
  nominations: CompetitionNominationDto[];
  robotGame?: CompetitionRobotGameEntryDto;
  nextCompetition?: CompetitionShortInfoDto;
}

export interface TeamProfileDto {
  profileName: string;
  profileUrl: string;
  avatarUrl?: string;
}

export interface CompetitionContactInfoDto {
  contactName?: string;
  contactEmail?: string;
}

export interface CompetitionDetailDto {
  /** @format int64 */
  id: number;
  season: SeasonDto;
  name: string;
  urlPart: string;
  slug?: string;
  type: CompetitionType;
  active: boolean;
  nextCompetition?: CompetitionShortInfoDto;
  previousCompetitions?: CompetitionShortInfoDto[];
  country?: string;
  /** @format date */
  date?: string;
  /** @format date */
  endDate?: string;
  contactInfo?: CompetitionContactInfoDto;
  location?: string;
  links: LinkDto[];
  registeredTeams: SeasonTeamDto[];
  results?: CompetitionResultsDto;
  /** @format int32 */
  registeredTeamCount?: number;
  /** @format int32 */
  maxTeamCount?: number;
  newApiCompetition: boolean;
}

export interface CompetitionResultsDto {
  places: CompetitionPlaceDto[];
  nominations: CompetitionNominationDto[];
  robotGameEntries: CompetitionRobotGameEntryDto[];
}

export interface SeasonTeamDto {
  /** @format int64 */
  id: number;
  season: SeasonDto;
  active: boolean;
  fllId: string;
  name: string;
  institution?: string;
  city?: string;
  country?: string;
  links: LinkDto[];
  seasonTeamProfile?: SeasonTeamProfileDto;
}

export interface CompetitionSearchResultDto {
  season: SeasonDto;
  urlPart: string;
  slug?: string;
  name: string;
  type: CompetitionType;
  country?: string;
  /** @format date */
  date?: string;
  /** @format date */
  endDate?: string;
  newApiCompetition: boolean;
}

export interface SearchResultItemDto {
  type?: SearchResultTypeDto;
  /** @format double */
  score?: number;
  competition?: CompetitionSearchResultDto;
  seasonTeam?: SeasonTeamSearchResultDto;
  teamProfile?: TeamProfileSearchResultDto;
  season?: SeasonSearchResultDto;
}

export interface SeasonSearchResultDto {
  id: string;
  name: string;
  /** @format int32 */
  startYear: number;
  active: boolean;
}

export interface SeasonTeamSearchResultDto {
  season: SeasonDto;
  fllId: string;
  name: string;
  institution?: string;
  city?: string;
  country?: string;
  seasonTeamProfile?: SeasonTeamProfileDto;
}

export interface TeamProfileSearchResultDto {
  profileName: string;
  profileUrl: string;
  avatarUrl?: string;
}

export interface TeamProfileDetailsDto {
  profileName: string;
  profileUrl: string;
  avatarUrl?: string;
  seasons: SeasonTeamDetailsDto[];
}

export interface NotificationPageDto {
  items: NotificationDto[];
  /** @format int64 */
  nextCursor?: number;
}

export interface PushConfigDto {
  enabled: boolean;
  publicKey?: string;
}

export interface OverallRobotGameEntryDto {
  /** @format int32 */
  rank: number;
  team: SeasonTeamSummaryDto;
  competition: CompetitionShortInfoDto;
  country?: string;
  qualified: boolean;
  /** @format int32 */
  bestScore: number;
  /** @format double */
  medianScore: number;
  /** @format double */
  averageScore: number;
  /** @format int32 */
  worstScore: number;
  /** @format int32 */
  preliminaryRound1: number;
  /** @format int32 */
  preliminaryRound2: number;
  /** @format int32 */
  preliminaryRound3: number;
  /** @format int32 */
  bestPreliminaryScore: number;
  /** @format int32 */
  roundOf16?: number;
  /** @format int32 */
  quarterFinal?: number;
  /** @format int32 */
  semiFinal?: number;
  /** @format int32 */
  final1?: number;
  /** @format int32 */
  final2?: number;
}

export interface SeasonTeamSummaryDto {
  /** @format int64 */
  id: number;
  season: SeasonDto;
  active: boolean;
  fllId: string;
  name: string;
  country?: string;
  seasonTeamProfile?: SeasonTeamProfileDto;
}

export interface StepUpMethodsDto {
  passkey?: boolean;
  totp?: boolean;
}

export interface ChallengeViewDto {
  kind: ChallengeViewKind;
  purpose?: LoginAttemptPurpose;
  options: number[];
  device?: string;
  /** @format date-time */
  requestedAt?: string;
  /** @format date-time */
  expiresAt?: string;
}

export interface SessionDto {
  /** @format uuid */
  id: string;
  device?: string;
  /** @format date-time */
  createdAt: string;
  /** @format date-time */
  lastUsedAt: string;
  current?: boolean;
  methods: AuthMethod[];
}

export interface AccountSecurityDto {
  passwordSet?: boolean;
  totpEnabled?: boolean;
  totpAvailable?: boolean;
  passkeys: PasskeyDto[];
}

import type {
  AxiosInstance,
  AxiosRequestConfig,
  AxiosResponse,
  HeadersDefaults,
  ResponseType,
} from "axios";
import axios from "axios";

export type QueryParamsType = Record<string | number, any>;

export interface FullRequestParams
  extends Omit<AxiosRequestConfig, "data" | "params" | "url" | "responseType"> {
  /** set parameter to `true` for call `securityWorker` for this request */
  secure?: boolean;
  /** request path */
  path: string;
  /** content type of request body */
  type?: ContentType;
  /** query params */
  query?: QueryParamsType;
  /** format of response (i.e. response.json() -> format: "json") */
  format?: ResponseType;
  /** request body */
  body?: unknown;
}

export type RequestParams = Omit<
  FullRequestParams,
  "body" | "method" | "query" | "path"
>;

export interface ApiConfig<SecurityDataType = unknown>
  extends Omit<AxiosRequestConfig, "data" | "cancelToken"> {
  securityWorker?: (
    securityData: SecurityDataType | null,
  ) => Promise<AxiosRequestConfig | void> | AxiosRequestConfig | void;
  secure?: boolean;
  format?: ResponseType;
}

export enum ContentType {
  Json = "application/json",
  JsonApi = "application/vnd.api+json",
  FormData = "multipart/form-data",
  UrlEncoded = "application/x-www-form-urlencoded",
  Text = "text/plain",
}

export class HttpClient<SecurityDataType = unknown> {
  public instance: AxiosInstance;
  private securityData: SecurityDataType | null = null;
  private securityWorker?: ApiConfig<SecurityDataType>["securityWorker"];
  private secure?: boolean;
  private format?: ResponseType;

  constructor({
    securityWorker,
    secure,
    format,
    ...axiosConfig
  }: ApiConfig<SecurityDataType> = {}) {
    this.instance = axios.create({
      ...axiosConfig,
      baseURL: axiosConfig.baseURL || "http://localhost:8080",
    });
    this.secure = secure;
    this.format = format;
    this.securityWorker = securityWorker;
  }

  public setSecurityData = (data: SecurityDataType | null) => {
    this.securityData = data;
  };

  protected mergeRequestParams(
    params1: AxiosRequestConfig,
    params2?: AxiosRequestConfig,
  ): AxiosRequestConfig {
    const method = params1.method || (params2 && params2.method);

    return {
      ...this.instance.defaults,
      ...params1,
      ...(params2 || {}),
      headers: {
        ...((method &&
          this.instance.defaults.headers[
            method.toLowerCase() as keyof HeadersDefaults
          ]) ||
          {}),
        ...(params1.headers || {}),
        ...((params2 && params2.headers) || {}),
      },
    };
  }

  protected stringifyFormItem(formItem: unknown) {
    if (typeof formItem === "object" && formItem !== null) {
      return JSON.stringify(formItem);
    } else {
      return `${formItem}`;
    }
  }

  protected createFormData(input: Record<string, unknown>): FormData {
    if (input instanceof FormData) {
      return input;
    }
    return Object.keys(input || {}).reduce((formData, key) => {
      const property = input[key];
      const propertyContent: any[] =
        property instanceof Array ? property : [property];

      for (const formItem of propertyContent) {
        const isFileType = formItem instanceof Blob || formItem instanceof File;
        formData.append(
          key,
          isFileType ? formItem : this.stringifyFormItem(formItem),
        );
      }

      return formData;
    }, new FormData());
  }

  public request = async <T = any, _E = any>({
    secure,
    path,
    type,
    query,
    format,
    body,
    ...params
  }: FullRequestParams): Promise<AxiosResponse<T>> => {
    const secureParams =
      ((typeof secure === "boolean" ? secure : this.secure) &&
        this.securityWorker &&
        (await this.securityWorker(this.securityData))) ||
      {};
    const requestParams = this.mergeRequestParams(params, secureParams);
    const responseFormat = format || this.format || undefined;

    if (
      type === ContentType.FormData &&
      body &&
      body !== null &&
      typeof body === "object"
    ) {
      body = this.createFormData(body as Record<string, unknown>);
    }

    if (
      type === ContentType.Text &&
      body &&
      body !== null &&
      typeof body !== "string"
    ) {
      body = JSON.stringify(body);
    }

    return this.instance.request({
      ...requestParams,
      headers: {
        ...(requestParams.headers || {}),
        ...(type ? { "Content-Type": type } : {}),
      },
      params: query,
      responseType: responseFormat,
      data: body,
      url: path,
    });
  };
}

/**
 * @title OpenAPI definition
 * @version v0
 * @baseUrl http://localhost:8080
 */
export class Api<
  SecurityDataType extends unknown,
> extends HttpClient<SecurityDataType> {
  api = {
    /**
     * No description
     *
     * @tags push-controller
     * @name ListPushSubscriptions
     * @request GET:/api/notifications/push/subscriptions
     */
    listPushSubscriptions: (params: RequestParams = {}) =>
      this.request<PushSubscriptionDto[], any>({
        path: `/api/notifications/push/subscriptions`,
        method: "GET",
        ...params,
      }),

    /**
     * No description
     *
     * @tags push-controller
     * @name RegisterPushSubscription
     * @request PUT:/api/notifications/push/subscriptions
     */
    registerPushSubscription: (
      data: PushSubscriptionRequestDto,
      params: RequestParams = {},
    ) =>
      this.request<PushSubscriptionDto, any>({
        path: `/api/notifications/push/subscriptions`,
        method: "PUT",
        body: data,
        type: ContentType.Json,
        ...params,
      }),

    /**
     * No description
     *
     * @tags notification-controller
     * @name GetNotificationPreferences
     * @request GET:/api/notifications/preferences
     */
    getNotificationPreferences: (params: RequestParams = {}) =>
      this.request<NotificationPreferenceDto[], any>({
        path: `/api/notifications/preferences`,
        method: "GET",
        ...params,
      }),

    /**
     * No description
     *
     * @tags notification-controller
     * @name UpdateNotificationPreference
     * @request PUT:/api/notifications/preferences
     */
    updateNotificationPreference: (
      data: NotificationPreferenceDto,
      params: RequestParams = {},
    ) =>
      this.request<NotificationPreferenceDto, any>({
        path: `/api/notifications/preferences`,
        method: "PUT",
        body: data,
        type: ContentType.Json,
        ...params,
      }),

    /**
     * No description
     *
     * @tags account-controller
     * @name SetPassword
     * @request PUT:/api/account/password
     */
    setPassword: (data: SetPasswordRequestDto, params: RequestParams = {}) =>
      this.request<UserDto, any>({
        path: `/api/account/password`,
        method: "PUT",
        body: data,
        type: ContentType.Json,
        ...params,
      }),

    /**
     * No description
     *
     * @tags account-controller
     * @name RemovePassword
     * @request DELETE:/api/account/password
     */
    removePassword: (params: RequestParams = {}) =>
      this.request<UserDto, any>({
        path: `/api/account/password`,
        method: "DELETE",
        ...params,
      }),

    /**
     * No description
     *
     * @tags notification-controller
     * @name MarkNotificationUnread
     * @request POST:/api/notifications/{id}/unread
     */
    markNotificationUnread: (id: number, params: RequestParams = {}) =>
      this.request<NotificationDto, any>({
        path: `/api/notifications/${id}/unread`,
        method: "POST",
        ...params,
      }),

    /**
     * No description
     *
     * @tags notification-controller
     * @name MarkNotificationRead
     * @request POST:/api/notifications/{id}/read
     */
    markNotificationRead: (id: number, params: RequestParams = {}) =>
      this.request<NotificationDto, any>({
        path: `/api/notifications/${id}/read`,
        method: "POST",
        ...params,
      }),

    /**
     * No description
     *
     * @tags notification-controller
     * @name MarkAllNotificationsRead
     * @request POST:/api/notifications/read-all
     */
    markAllNotificationsRead: (
      data: MarkAllReadRequestDto,
      params: RequestParams = {},
    ) =>
      this.request<UnreadCountDto, any>({
        path: `/api/notifications/read-all`,
        method: "POST",
        body: data,
        type: ContentType.Json,
        ...params,
      }),

    /**
     * No description
     *
     * @tags push-controller
     * @name SendTestPush
     * @request POST:/api/notifications/push/test
     */
    sendTestPush: (params: RequestParams = {}) =>
      this.request<PushTestResultDto, any>({
        path: `/api/notifications/push/test`,
        method: "POST",
        ...params,
      }),

    /**
     * No description
     *
     * @tags push-controller
     * @name UnregisterPushSubscription
     * @request POST:/api/notifications/push/subscriptions/unregister
     */
    unregisterPushSubscription: (
      data: PushUnregisterRequestDto,
      params: RequestParams = {},
    ) =>
      this.request<void, any>({
        path: `/api/notifications/push/subscriptions/unregister`,
        method: "POST",
        body: data,
        type: ContentType.Json,
        ...params,
      }),

    /**
     * No description
     *
     * @tags step-up-controller
     * @name StepUpWithTotp
     * @request POST:/api/auth/step-up/totp
     */
    stepUpWithTotp: (data: TotpCodeRequestDto, params: RequestParams = {}) =>
      this.request<void, any>({
        path: `/api/auth/step-up/totp`,
        method: "POST",
        body: data,
        type: ContentType.Json,
        ...params,
      }),

    /**
     * No description
     *
     * @tags step-up-controller
     * @name StepUpWithPasskey
     * @request POST:/api/auth/step-up/passkey
     */
    stepUpWithPasskey: (data: PasskeyResponseDto, params: RequestParams = {}) =>
      this.request<void, any>({
        path: `/api/auth/step-up/passkey`,
        method: "POST",
        body: data,
        type: ContentType.Json,
        ...params,
      }),

    /**
     * No description
     *
     * @tags step-up-controller
     * @name StepUpPasskeyOptions
     * @request POST:/api/auth/step-up/passkey/options
     */
    stepUpPasskeyOptions: (params: RequestParams = {}) =>
      this.request<PasskeyAuthenticationOptionsDto, any>({
        path: `/api/auth/step-up/passkey/options`,
        method: "POST",
        ...params,
      }),

    /**
     * No description
     *
     * @tags step-up-controller
     * @name StartStepUpChallenge
     * @request POST:/api/auth/step-up/challenge
     */
    startStepUpChallenge: (
      data: PushEndpointRequestDto,
      params: RequestParams = {},
    ) =>
      this.request<LoginAttemptStateDto, any>({
        path: `/api/auth/step-up/challenge`,
        method: "POST",
        body: data,
        type: ContentType.Json,
        ...params,
      }),

    /**
     * No description
     *
     * @tags auth-controller
     * @name PasskeyLoginOptions
     * @request POST:/api/auth/passkey/options
     */
    passkeyLoginOptions: (params: RequestParams = {}) =>
      this.request<PasskeyAuthenticationOptionsDto, any>({
        path: `/api/auth/passkey/options`,
        method: "POST",
        ...params,
      }),

    /**
     * No description
     *
     * @tags auth-controller
     * @name LoginWithPasskey
     * @request POST:/api/auth/passkey/login
     */
    loginWithPasskey: (data: PasskeyResponseDto, params: RequestParams = {}) =>
      this.request<UserDto, any>({
        path: `/api/auth/passkey/login`,
        method: "POST",
        body: data,
        type: ContentType.Json,
        ...params,
      }),

    /**
     * No description
     *
     * @tags auth-controller
     * @name Logout
     * @request POST:/api/auth/logout
     */
    logout: (params: RequestParams = {}) =>
      this.request<void, any>({
        path: `/api/auth/logout`,
        method: "POST",
        ...params,
      }),

    /**
     * No description
     *
     * @tags auth-controller
     * @name LoginWithPassword
     * @request POST:/api/auth/login/password
     */
    loginWithPassword: (
      data: PasswordLoginRequestDto,
      params: RequestParams = {},
    ) =>
      this.request<LoginResultDto, any>({
        path: `/api/auth/login/password`,
        method: "POST",
        body: data,
        type: ContentType.Json,
        ...params,
      }),

    /**
     * No description
     *
     * @tags auth-controller
     * @name LoginWithPasswordAndTotp
     * @request POST:/api/auth/login/password/totp
     */
    loginWithPasswordAndTotp: (
      data: TotpCodeRequestDto,
      params: RequestParams = {},
    ) =>
      this.request<LoginResultDto, any>({
        path: `/api/auth/login/password/totp`,
        method: "POST",
        body: data,
        type: ContentType.Json,
        ...params,
      }),

    /**
     * No description
     *
     * @tags login-attempt-controller
     * @name RejectLink
     * @request POST:/api/auth/link/reject
     */
    rejectLink: (data: LinkTokenRequestDto, params: RequestParams = {}) =>
      this.request<void, any>({
        path: `/api/auth/link/reject`,
        method: "POST",
        body: data,
        type: ContentType.Json,
        ...params,
      }),

    /**
     * No description
     *
     * @tags login-attempt-controller
     * @name LinkOpened
     * @request POST:/api/auth/link/opened
     */
    linkOpened: (data: LinkTokenRequestDto, params: RequestParams = {}) =>
      this.request<void, any>({
        path: `/api/auth/link/opened`,
        method: "POST",
        body: data,
        type: ContentType.Json,
        ...params,
      }),

    /**
     * No description
     *
     * @tags login-attempt-controller
     * @name CompleteLink
     * @request POST:/api/auth/link/complete
     */
    completeLink: (data: LinkTokenRequestDto, params: RequestParams = {}) =>
      this.request<AttemptCompletionDto, any>({
        path: `/api/auth/link/complete`,
        method: "POST",
        body: data,
        type: ContentType.Json,
        ...params,
      }),

    /**
     * No description
     *
     * @tags login-attempt-controller
     * @name ClaimLinkHere
     * @request POST:/api/auth/link/claim-here
     */
    claimLinkHere: (data: LinkTokenRequestDto, params: RequestParams = {}) =>
      this.request<UserDto, any>({
        path: `/api/auth/link/claim-here`,
        method: "POST",
        body: data,
        type: ContentType.Json,
        ...params,
      }),

    /**
     * No description
     *
     * @tags login-attempt-controller
     * @name ApproveLink
     * @request POST:/api/auth/link/approve
     */
    approveLink: (data: LinkApproveRequestDto, params: RequestParams = {}) =>
      this.request<ChallengePickResultDto, any>({
        path: `/api/auth/link/approve`,
        method: "POST",
        body: data,
        type: ContentType.Json,
        ...params,
      }),

    /**
     * No description
     *
     * @tags auth-controller
     * @name Identify
     * @request POST:/api/auth/identify
     */
    identify: (data: IdentifyRequestDto, params: RequestParams = {}) =>
      this.request<IdentifyResponseDto, any>({
        path: `/api/auth/identify`,
        method: "POST",
        body: data,
        type: ContentType.Json,
        ...params,
      }),

    /**
     * No description
     *
     * @tags auth-controller
     * @name CompleteProfile
     * @request POST:/api/auth/complete-profile
     */
    completeProfile: (
      data: CompleteProfileRequestDto,
      params: RequestParams = {},
    ) =>
      this.request<UserDto, any>({
        path: `/api/auth/complete-profile`,
        method: "POST",
        body: data,
        type: ContentType.Json,
        ...params,
      }),

    /**
     * No description
     *
     * @tags auth-controller
     * @name StartAttempt
     * @request POST:/api/auth/attempts
     */
    startAttempt: (data: StartAttemptRequestDto, params: RequestParams = {}) =>
      this.request<LoginAttemptStateDto, any>({
        path: `/api/auth/attempts`,
        method: "POST",
        body: data,
        type: ContentType.Json,
        ...params,
      }),

    /**
     * No description
     *
     * @tags login-attempt-controller
     * @name SendAttemptEmail
     * @request POST:/api/auth/attempts/current/email
     */
    sendAttemptEmail: (params: RequestParams = {}) =>
      this.request<void, any>({
        path: `/api/auth/attempts/current/email`,
        method: "POST",
        ...params,
      }),

    /**
     * No description
     *
     * @tags login-attempt-controller
     * @name CompleteCurrentAttempt
     * @request POST:/api/auth/attempts/current/complete
     */
    completeCurrentAttempt: (params: RequestParams = {}) =>
      this.request<AttemptCompletionDto, any>({
        path: `/api/auth/attempts/current/complete`,
        method: "POST",
        ...params,
      }),

    /**
     * No description
     *
     * @tags login-attempt-controller
     * @name RejectPush
     * @request POST:/api/auth/approvals/{attemptId}/reject
     */
    rejectPush: (attemptId: string, params: RequestParams = {}) =>
      this.request<void, any>({
        path: `/api/auth/approvals/${attemptId}/reject`,
        method: "POST",
        ...params,
      }),

    /**
     * No description
     *
     * @tags login-attempt-controller
     * @name PushApprovalOpened
     * @request POST:/api/auth/approvals/{attemptId}/opened
     */
    pushApprovalOpened: (attemptId: string, params: RequestParams = {}) =>
      this.request<void, any>({
        path: `/api/auth/approvals/${attemptId}/opened`,
        method: "POST",
        ...params,
      }),

    /**
     * No description
     *
     * @tags login-attempt-controller
     * @name ApprovePush
     * @request POST:/api/auth/approvals/{attemptId}/approve
     */
    approvePush: (
      attemptId: string,
      data: ApprovalPickRequestDto,
      params: RequestParams = {},
    ) =>
      this.request<ChallengePickResultDto, any>({
        path: `/api/auth/approvals/${attemptId}/approve`,
        method: "POST",
        body: data,
        type: ContentType.Json,
        ...params,
      }),

    /**
     * No description
     *
     * @tags admin-notification-controller
     * @name SendAnnouncement
     * @request POST:/api/admin/notifications/announcement
     */
    sendAnnouncement: (
      data: AnnouncementRequestDto,
      params: RequestParams = {},
    ) =>
      this.request<AnnouncementResultDto, any>({
        path: `/api/admin/notifications/announcement`,
        method: "POST",
        body: data,
        type: ContentType.Json,
        ...params,
      }),

    /**
     * No description
     *
     * @tags admin-mail-controller
     * @name SendTestMail
     * @request POST:/api/admin/mail/test
     */
    sendTestMail: (data: TestMailRequestDto, params: RequestParams = {}) =>
      this.request<object, any>({
        path: `/api/admin/mail/test`,
        method: "POST",
        body: data,
        type: ContentType.Json,
        ...params,
      }),

    /**
     * No description
     *
     * @tags admin-mail-controller
     * @name Requeue
     * @request POST:/api/admin/mail/outbox/{id}/requeue
     */
    requeue: (id: number, params: RequestParams = {}) =>
      this.request<MailOutboxEntryDto, any>({
        path: `/api/admin/mail/outbox/${id}/requeue`,
        method: "POST",
        ...params,
      }),

    /**
     * No description
     *
     * @tags account-controller
     * @name BeginTotp
     * @request POST:/api/account/totp
     */
    beginTotp: (params: RequestParams = {}) =>
      this.request<TotpEnrollmentDto, any>({
        path: `/api/account/totp`,
        method: "POST",
        ...params,
      }),

    /**
     * No description
     *
     * @tags account-controller
     * @name DisableTotp
     * @request DELETE:/api/account/totp
     */
    disableTotp: (params: RequestParams = {}) =>
      this.request<UserDto, any>({
        path: `/api/account/totp`,
        method: "DELETE",
        ...params,
      }),

    /**
     * No description
     *
     * @tags account-controller
     * @name ConfirmTotp
     * @request POST:/api/account/totp/confirm
     */
    confirmTotp: (data: TotpCodeRequestDto, params: RequestParams = {}) =>
      this.request<UserDto, any>({
        path: `/api/account/totp/confirm`,
        method: "POST",
        body: data,
        type: ContentType.Json,
        ...params,
      }),

    /**
     * No description
     *
     * @tags account-controller
     * @name RevokeOtherSessions
     * @request POST:/api/account/sessions/revoke-others
     */
    revokeOtherSessions: (params: RequestParams = {}) =>
      this.request<void, any>({
        path: `/api/account/sessions/revoke-others`,
        method: "POST",
        ...params,
      }),

    /**
     * No description
     *
     * @tags account-controller
     * @name ListPasskeys
     * @request GET:/api/account/passkeys
     */
    listPasskeys: (params: RequestParams = {}) =>
      this.request<PasskeyDto[], any>({
        path: `/api/account/passkeys`,
        method: "GET",
        ...params,
      }),

    /**
     * No description
     *
     * @tags account-controller
     * @name RegisterPasskey
     * @request POST:/api/account/passkeys
     */
    registerPasskey: (data: PasskeyResponseDto, params: RequestParams = {}) =>
      this.request<PasskeyDto, any>({
        path: `/api/account/passkeys`,
        method: "POST",
        body: data,
        type: ContentType.Json,
        ...params,
      }),

    /**
     * No description
     *
     * @tags account-controller
     * @name PasskeyRegistrationOptions
     * @request POST:/api/account/passkeys/options
     */
    passkeyRegistrationOptions: (params: RequestParams = {}) =>
      this.request<PasskeyRegistrationOptionsDto, any>({
        path: `/api/account/passkeys/options`,
        method: "POST",
        ...params,
      }),

    /**
     * No description
     *
     * @tags account-controller
     * @name RequestEmailChange
     * @request POST:/api/account/email
     */
    requestEmailChange: (
      data: EmailChangeRequestDto,
      params: RequestParams = {},
    ) =>
      this.request<void, any>({
        path: `/api/account/email`,
        method: "POST",
        body: data,
        type: ContentType.Json,
        ...params,
      }),

    /**
     * No description
     *
     * @tags account-controller
     * @name ConfirmEmailChange
     * @request POST:/api/account/email/confirm
     */
    confirmEmailChange: (
      data: LinkTokenRequestDto,
      params: RequestParams = {},
    ) =>
      this.request<void, any>({
        path: `/api/account/email/confirm`,
        method: "POST",
        body: data,
        type: ContentType.Json,
        ...params,
      }),

    /**
     * No description
     *
     * @tags account-controller
     * @name DeletePasskey
     * @request DELETE:/api/account/passkeys/{id}
     */
    deletePasskey: (id: number, params: RequestParams = {}) =>
      this.request<void, any>({
        path: `/api/account/passkeys/${id}`,
        method: "DELETE",
        ...params,
      }),

    /**
     * No description
     *
     * @tags account-controller
     * @name RenamePasskey
     * @request PATCH:/api/account/passkeys/{id}
     */
    renamePasskey: (
      id: number,
      data: RenamePasskeyRequestDto,
      params: RequestParams = {},
    ) =>
      this.request<PasskeyDto, any>({
        path: `/api/account/passkeys/${id}`,
        method: "PATCH",
        body: data,
        type: ContentType.Json,
        ...params,
      }),

    /**
     * No description
     *
     * @tags sync-test-controller
     * @name TriggerFullSync
     * @request GET:/api/syncer/trigger-sync
     */
    triggerFullSync: (params: RequestParams = {}) =>
      this.request<string, any>({
        path: `/api/syncer/trigger-sync`,
        method: "GET",
        ...params,
      }),

    /**
     * No description
     *
     * @tags season-team-controller
     * @name GetTeamDetails
     * @request GET:/api/seasons/{seasonId}/teams/{fllId}
     */
    getTeamDetails: (
      seasonId: string,
      fllId: string,
      params: RequestParams = {},
    ) =>
      this.request<SeasonTeamDetailsDto, any>({
        path: `/api/seasons/${seasonId}/teams/${fllId}`,
        method: "GET",
        ...params,
      }),

    /**
     * No description
     *
     * @tags competition-controller
     * @name GetCompetitionsForSeason
     * @request GET:/api/seasons/{seasonId}/competitions
     */
    getCompetitionsForSeason: (seasonId: string, params: RequestParams = {}) =>
      this.request<CompetitionShortInfoDto[], any>({
        path: `/api/seasons/${seasonId}/competitions`,
        method: "GET",
        ...params,
      }),

    /**
     * No description
     *
     * @tags competition-controller
     * @name GetCompetitionDetails
     * @request GET:/api/seasons/{seasonId}/competitions/{urlPart}
     */
    getCompetitionDetails: (
      seasonId: string,
      urlPart: string,
      params: RequestParams = {},
    ) =>
      this.request<CompetitionDetailDto, any>({
        path: `/api/seasons/${seasonId}/competitions/${urlPart}`,
        method: "GET",
        ...params,
      }),

    /**
     * No description
     *
     * @tags season-controller
     * @name GetAllSeasons
     * @request GET:/api/season
     */
    getAllSeasons: (params: RequestParams = {}) =>
      this.request<SeasonDto[], any>({
        path: `/api/season`,
        method: "GET",
        ...params,
      }),

    /**
     * No description
     *
     * @tags search-controller
     * @name GlobalSearch
     * @request GET:/api/search
     */
    globalSearch: (
      query: {
        q: string;
        seasonId?: string;
      },
      params: RequestParams = {},
    ) =>
      this.request<SearchResultItemDto[], any>({
        path: `/api/search`,
        method: "GET",
        query: query,
        ...params,
      }),

    /**
     * No description
     *
     * @tags scraper-test-controller
     * @name Test
     * @request GET:/api/scraper/test
     */
    test: (params: RequestParams = {}) =>
      this.request<Record<string, string>, any>({
        path: `/api/scraper/test`,
        method: "GET",
        ...params,
      }),

    /**
     * No description
     *
     * @tags scraper-test-controller
     * @name SyncCompetition
     * @request GET:/api/scraper/sync-competition/{seasonId}/{urlPart}
     */
    syncCompetition: (
      seasonId: string,
      urlPart: string,
      query?: {
        /** @default false */
        ignoreHashes?: boolean;
      },
      params: RequestParams = {},
    ) =>
      this.request<string, any>({
        path: `/api/scraper/sync-competition/${seasonId}/${urlPart}`,
        method: "GET",
        query: query,
        ...params,
      }),

    /**
     * No description
     *
     * @tags scraper-test-controller
     * @name ForceQuickSync
     * @request GET:/api/scraper/force-quick
     */
    forceQuickSync: (params: RequestParams = {}) =>
      this.request<string, any>({
        path: `/api/scraper/force-quick`,
        method: "GET",
        ...params,
      }),

    /**
     * No description
     *
     * @tags scraper-test-controller
     * @name ForceFullSync
     * @request GET:/api/scraper/force-full
     */
    forceFullSync: (
      query?: {
        /** @default false */
        ignoreHashes?: boolean;
      },
      params: RequestParams = {},
    ) =>
      this.request<string, any>({
        path: `/api/scraper/force-full`,
        method: "GET",
        query: query,
        ...params,
      }),

    /**
     * No description
     *
     * @tags scraper-test-controller
     * @name FetchOldSeasonData
     * @request GET:/api/scraper/fetch-old/{seasonId}
     */
    fetchOldSeasonData: (
      seasonId: string,
      query?: {
        /** @default false */
        ignoreHashes?: boolean;
        /** @default false */
        skipWithHashes?: boolean;
      },
      params: RequestParams = {},
    ) =>
      this.request<string, any>({
        path: `/api/scraper/fetch-old/${seasonId}`,
        method: "GET",
        query: query,
        ...params,
      }),

    /**
     * No description
     *
     * @tags team-profile-controller
     * @name GetTeamProfileDetails
     * @request GET:/api/profiles/{profileUrl}
     */
    getTeamProfileDetails: (profileUrl: string, params: RequestParams = {}) =>
      this.request<TeamProfileDetailsDto, any>({
        path: `/api/profiles/${profileUrl}`,
        method: "GET",
        ...params,
      }),

    /**
     * No description
     *
     * @tags notification-controller
     * @name ListNotifications
     * @request GET:/api/notifications
     */
    listNotifications: (
      query?: {
        /** @format int64 */
        cursor?: number;
        /**
         * @format int32
         * @default 20
         */
        size?: number;
        /** @default false */
        unreadOnly?: boolean;
      },
      params: RequestParams = {},
    ) =>
      this.request<NotificationPageDto, any>({
        path: `/api/notifications`,
        method: "GET",
        query: query,
        ...params,
      }),

    /**
     * No description
     *
     * @tags notification-controller
     * @name GetUnreadCount
     * @request GET:/api/notifications/unread-count
     */
    getUnreadCount: (params: RequestParams = {}) =>
      this.request<UnreadCountDto, any>({
        path: `/api/notifications/unread-count`,
        method: "GET",
        ...params,
      }),

    /**
     * No description
     *
     * @tags notification-controller
     * @name ReplayNotifications
     * @request GET:/api/notifications/replay
     */
    replayNotifications: (
      query: {
        /** @format int64 */
        after: number;
      },
      params: RequestParams = {},
    ) =>
      this.request<NotificationDto[], any>({
        path: `/api/notifications/replay`,
        method: "GET",
        query: query,
        ...params,
      }),

    /**
     * No description
     *
     * @tags push-controller
     * @name GetPushConfig
     * @request GET:/api/notifications/push/config
     */
    getPushConfig: (params: RequestParams = {}) =>
      this.request<PushConfigDto, any>({
        path: `/api/notifications/push/config`,
        method: "GET",
        ...params,
      }),

    /**
     * No description
     *
     * @tags leaderboard-controller
     * @name GetOverallRobotGameLeaderboard
     * @request GET:/api/leaderboard/overall-robot-game
     */
    getOverallRobotGameLeaderboard: (
      query?: {
        seasonId?: string;
      },
      params: RequestParams = {},
    ) =>
      this.request<OverallRobotGameEntryDto[], any>({
        path: `/api/leaderboard/overall-robot-game`,
        method: "GET",
        query: query,
        ...params,
      }),

    /**
     * No description
     *
     * @tags image-controller
     * @name DownloadImage
     * @request GET:/api/images/{id}
     */
    downloadImage: (id: string, params: RequestParams = {}) =>
      this.request<string, any>({
        path: `/api/images/${id}`,
        method: "GET",
        ...params,
      }),

    /**
     * No description
     *
     * @tags step-up-controller
     * @name StepUpMethods
     * @request GET:/api/auth/step-up/methods
     */
    stepUpMethods: (params: RequestParams = {}) =>
      this.request<StepUpMethodsDto, any>({
        path: `/api/auth/step-up/methods`,
        method: "GET",
        ...params,
      }),

    /**
     * No description
     *
     * @tags auth-controller
     * @name GetCurrentUser
     * @request GET:/api/auth/me
     */
    getCurrentUser: (params: RequestParams = {}) =>
      this.request<UserDto, any>({
        path: `/api/auth/me`,
        method: "GET",
        ...params,
      }),

    /**
     * No description
     *
     * @tags login-attempt-controller
     * @name ViewLink
     * @request GET:/api/auth/link
     */
    viewLink: (
      query: {
        token: string;
      },
      params: RequestParams = {},
    ) =>
      this.request<ChallengeViewDto, any>({
        path: `/api/auth/link`,
        method: "GET",
        query: query,
        ...params,
      }),

    /**
     * No description
     *
     * @tags login-attempt-controller
     * @name GetCurrentAttempt
     * @request GET:/api/auth/attempts/current
     */
    getCurrentAttempt: (params: RequestParams = {}) =>
      this.request<LoginAttemptStateDto, any>({
        path: `/api/auth/attempts/current`,
        method: "GET",
        ...params,
      }),

    /**
     * No description
     *
     * @tags login-attempt-controller
     * @name CancelCurrentAttempt
     * @request DELETE:/api/auth/attempts/current
     */
    cancelCurrentAttempt: (params: RequestParams = {}) =>
      this.request<void, any>({
        path: `/api/auth/attempts/current`,
        method: "DELETE",
        ...params,
      }),

    /**
     * No description
     *
     * @tags login-attempt-controller
     * @name ViewPushApproval
     * @request GET:/api/auth/approvals/{attemptId}
     */
    viewPushApproval: (attemptId: string, params: RequestParams = {}) =>
      this.request<ChallengeViewDto, any>({
        path: `/api/auth/approvals/${attemptId}`,
        method: "GET",
        ...params,
      }),

    /**
     * No description
     *
     * @tags admin-mail-controller
     * @name ListOutbox
     * @request GET:/api/admin/mail/outbox
     */
    listOutbox: (
      query?: {
        /** @default "FAILED" */
        status?: MailOutboxStatus;
      },
      params: RequestParams = {},
    ) =>
      this.request<MailOutboxEntryDto[], any>({
        path: `/api/admin/mail/outbox`,
        method: "GET",
        query: query,
        ...params,
      }),

    /**
     * No description
     *
     * @tags account-controller
     * @name ListSessions
     * @request GET:/api/account/sessions
     */
    listSessions: (params: RequestParams = {}) =>
      this.request<SessionDto[], any>({
        path: `/api/account/sessions`,
        method: "GET",
        ...params,
      }),

    /**
     * No description
     *
     * @tags account-controller
     * @name GetAccountSecurity
     * @request GET:/api/account/security
     */
    getAccountSecurity: (params: RequestParams = {}) =>
      this.request<AccountSecurityDto, any>({
        path: `/api/account/security`,
        method: "GET",
        ...params,
      }),

    /**
     * No description
     *
     * @tags push-controller
     * @name DeletePushSubscription
     * @request DELETE:/api/notifications/push/subscriptions/{id}
     */
    deletePushSubscription: (id: number, params: RequestParams = {}) =>
      this.request<void, any>({
        path: `/api/notifications/push/subscriptions/${id}`,
        method: "DELETE",
        ...params,
      }),

    /**
     * No description
     *
     * @tags account-controller
     * @name DeleteAccount
     * @request DELETE:/api/account
     */
    deleteAccount: (params: RequestParams = {}) =>
      this.request<void, any>({
        path: `/api/account`,
        method: "DELETE",
        ...params,
      }),

    /**
     * No description
     *
     * @tags account-controller
     * @name RevokeSession
     * @request DELETE:/api/account/sessions/{id}
     */
    revokeSession: (id: string, params: RequestParams = {}) =>
      this.request<void, any>({
        path: `/api/account/sessions/${id}`,
        method: "DELETE",
        ...params,
      }),
  };
}

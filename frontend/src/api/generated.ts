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

export enum MailOutboxStatus {
  PENDING = "PENDING",
  SENDING = "SENDING",
  SENT = "SENT",
  FAILED = "FAILED",
  CANCELLED = "CANCELLED",
}

export enum RegistrationStatus {
  PENDING = "PENDING",
  AWAITING_CONFIRMATION = "AWAITING_CONFIRMATION",
  VERIFIED = "VERIFIED",
  CLAIMED_ELSEWHERE = "CLAIMED_ELSEWHERE",
  CANCELLED = "CANCELLED",
  UNKNOWN = "UNKNOWN",
}

export enum ChallengeKind {
  CHALLENGE = "CHALLENGE",
  SAME_BROWSER = "SAME_BROWSER",
  NO_CHALLENGE = "NO_CHALLENGE",
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

export enum RegistrationStartStatus {
  PENDING = "PENDING",
  ALREADY_REGISTERED = "ALREADY_REGISTERED",
}

export interface VerifyEmailRequestDto {
  /** @minLength 1 */
  token: string;
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

export interface RegistrationResponseDto {
  message?: string;
}

export interface ConfirmChallengeRequestDto {
  /** @minLength 1 */
  token: string;
  /**
   * @format int32
   * @min 10
   * @max 99
   */
  number: number;
}

export interface ResetPasswordRequestDto {
  /** @minLength 1 */
  token: string;
  /**
   * @minLength 8
   * @maxLength 2147483647
   */
  password: string;
}

export interface ResendVerificationRequestDto {
  /**
   * @format email
   * @minLength 1
   */
  email: string;
  locale?: string;
}

export interface ClaimRegistrationRequestDto {
  /** @minLength 1 */
  registrationId: string;
}

export interface RegisterRequestDto {
  /**
   * @format email
   * @minLength 1
   */
  email: string;
  locale?: string;
}

export interface RegistrationStartResponseDto {
  status: RegistrationStartStatus;
  registrationId?: string;
  /** @format int32 */
  challengeNumber?: number;
}

export interface LoginRequestDto {
  /** @minLength 1 */
  email: string;
  /** @minLength 1 */
  password: string;
}

export interface EmailLinkRequestDto {
  /**
   * @format email
   * @minLength 1
   */
  email: string;
  locale?: string;
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
  /**
   * @minLength 8
   * @maxLength 2147483647
   */
  password?: string;
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

export interface VerificationChallengeResponseDto {
  kind?: ChallengeKind;
  numbers?: number[];
  email?: string;
}

export interface RegistrationStatusResponseDto {
  status: RegistrationStatus;
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
     * @tags auth-controller
     * @name VerifyEmail
     * @request POST:/api/auth/verify-email
     */
    verifyEmail: (data: VerifyEmailRequestDto, params: RequestParams = {}) =>
      this.request<UserDto, any>({
        path: `/api/auth/verify-email`,
        method: "POST",
        body: data,
        type: ContentType.Json,
        ...params,
      }),

    /**
     * No description
     *
     * @tags auth-controller
     * @name RejectRegistration
     * @request POST:/api/auth/verification-reject
     */
    rejectRegistration: (
      data: VerifyEmailRequestDto,
      params: RequestParams = {},
    ) =>
      this.request<RegistrationResponseDto, any>({
        path: `/api/auth/verification-reject`,
        method: "POST",
        body: data,
        type: ContentType.Json,
        ...params,
      }),

    /**
     * No description
     *
     * @tags auth-controller
     * @name ConfirmChallenge
     * @request POST:/api/auth/verification-confirm
     */
    confirmChallenge: (
      data: ConfirmChallengeRequestDto,
      params: RequestParams = {},
    ) =>
      this.request<RegistrationResponseDto, any>({
        path: `/api/auth/verification-confirm`,
        method: "POST",
        body: data,
        type: ContentType.Json,
        ...params,
      }),

    /**
     * No description
     *
     * @tags auth-controller
     * @name ResetPassword
     * @request POST:/api/auth/reset-password
     */
    resetPassword: (
      data: ResetPasswordRequestDto,
      params: RequestParams = {},
    ) =>
      this.request<UserDto, any>({
        path: `/api/auth/reset-password`,
        method: "POST",
        body: data,
        type: ContentType.Json,
        ...params,
      }),

    /**
     * No description
     *
     * @tags auth-controller
     * @name ResendVerification
     * @request POST:/api/auth/resend-verification
     */
    resendVerification: (
      data: ResendVerificationRequestDto,
      params: RequestParams = {},
    ) =>
      this.request<RegistrationResponseDto, any>({
        path: `/api/auth/resend-verification`,
        method: "POST",
        body: data,
        type: ContentType.Json,
        ...params,
      }),

    /**
     * No description
     *
     * @tags auth-controller
     * @name ClaimRegistration
     * @request POST:/api/auth/registration-claim
     */
    claimRegistration: (
      data: ClaimRegistrationRequestDto,
      params: RequestParams = {},
    ) =>
      this.request<UserDto, any>({
        path: `/api/auth/registration-claim`,
        method: "POST",
        body: data,
        type: ContentType.Json,
        ...params,
      }),

    /**
     * No description
     *
     * @tags auth-controller
     * @name Register
     * @request POST:/api/auth/register
     */
    register: (data: RegisterRequestDto, params: RequestParams = {}) =>
      this.request<RegistrationStartResponseDto, any>({
        path: `/api/auth/register`,
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
      this.request<string, any>({
        path: `/api/auth/logout`,
        method: "POST",
        ...params,
      }),

    /**
     * No description
     *
     * @tags auth-controller
     * @name Login
     * @request POST:/api/auth/login
     */
    login: (data: LoginRequestDto, params: RequestParams = {}) =>
      this.request<UserDto, any>({
        path: `/api/auth/login`,
        method: "POST",
        body: data,
        type: ContentType.Json,
        ...params,
      }),

    /**
     * No description
     *
     * @tags auth-controller
     * @name RequestLoginLink
     * @request POST:/api/auth/login-link
     */
    requestLoginLink: (data: EmailLinkRequestDto, params: RequestParams = {}) =>
      this.request<RegistrationResponseDto, any>({
        path: `/api/auth/login-link`,
        method: "POST",
        body: data,
        type: ContentType.Json,
        ...params,
      }),

    /**
     * No description
     *
     * @tags auth-controller
     * @name ConsumeLoginLink
     * @request POST:/api/auth/login-link/consume
     */
    consumeLoginLink: (
      data: VerifyEmailRequestDto,
      params: RequestParams = {},
    ) =>
      this.request<UserDto, any>({
        path: `/api/auth/login-link/consume`,
        method: "POST",
        body: data,
        type: ContentType.Json,
        ...params,
      }),

    /**
     * No description
     *
     * @tags auth-controller
     * @name ForgotPassword
     * @request POST:/api/auth/forgot-password
     */
    forgotPassword: (data: EmailLinkRequestDto, params: RequestParams = {}) =>
      this.request<RegistrationResponseDto, any>({
        path: `/api/auth/forgot-password`,
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
     * @tags auth-controller
     * @name VerificationChallenge
     * @request GET:/api/auth/verification-challenge
     */
    verificationChallenge: (
      query: {
        token: string;
      },
      params: RequestParams = {},
    ) =>
      this.request<VerificationChallengeResponseDto, any>({
        path: `/api/auth/verification-challenge`,
        method: "GET",
        query: query,
        ...params,
      }),

    /**
     * No description
     *
     * @tags auth-controller
     * @name RegistrationStatus
     * @request GET:/api/auth/registration-status
     */
    registrationStatus: (
      query: {
        registrationId: string;
      },
      params: RequestParams = {},
    ) =>
      this.request<RegistrationStatusResponseDto, any>({
        path: `/api/auth/registration-status`,
        method: "GET",
        query: query,
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
  };
}

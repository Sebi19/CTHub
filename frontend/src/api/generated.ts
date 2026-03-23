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

export interface LoginRequestDto {
  email: string;
  password: string;
}

export interface UserDto {
  /** @format int64 */
  id: number;
  email: string;
  role?: string;
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
  prelimRank?: number;
}

export interface CompetitionShortInfoDto {
  /** @format int64 */
  id: number;
  season: SeasonDto;
  name: string;
  urlPart: string;
  type: CompetitionType;
  active: boolean;
  resultsAvailable: boolean;
  country?: string;
  /** @format date */
  date?: string;
  /** @format date */
  endDate?: string;
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

export interface TeamProfileDetailsDto {
  profileName: string;
  profileUrl: string;
  avatarUrl?: string;
  seasons: SeasonTeamDetailsDto[];
}

export interface OverallRobotGameEntryDto {
  /** @format int32 */
  rank: number;
  team: SeasonTeamDto;
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
      this.request<string[], any>({
        path: `/api/images/${id}`,
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
  };
}

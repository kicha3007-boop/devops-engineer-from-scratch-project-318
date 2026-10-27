import { type DataProvider, fetchUtils, type Identifier } from "react-admin";

const apiUrl = import.meta.env.VITE_API_URL ?? "/api";
const fileUploadUrl = `${apiUrl}/files/upload`;

const httpClient = (url: string, options: fetchUtils.Options = {}) => {
  if (!options.headers) {
    options.headers = new Headers({ Accept: "application/json" });
  }

  if (!(options.body instanceof FormData)) {
    (options.headers as Headers).set("Content-Type", "application/json");
  }

  return fetchUtils.fetchJson(url, options);
};

const getCollectionUrl = (resource: string) => `${apiUrl}/${resource}`;
const getRecordUrl = (resource: string, id: Identifier) =>
  `${getCollectionUrl(resource)}/${id}`;

type JsonRecord = Record<string, unknown>;
type ImageFieldValue = { rawFile?: unknown };
type MutablePayload = JsonRecord & {
  image?: ImageFieldValue | ImageFieldValue[];
  imageUrl?: string;
  imageKey?: string;
};

const hasRawFile = (value: unknown): value is { rawFile: File } => {
  if (typeof value !== "object" || value === null || !("rawFile" in value)) {
    return false;
  }

  return (value as { rawFile?: unknown }).rawFile instanceof File;
};

const asRecord = (payload: unknown): Record<string, unknown> | null =>
  typeof payload === "object" && payload !== null
    ? (payload as Record<string, unknown>)
    : null;

const extractCollection = (payload: unknown): JsonRecord[] => {
  if (Array.isArray(payload)) {
    return payload as JsonRecord[];
  }

  const record = asRecord(payload);
  if (record && Array.isArray(record.data)) {
    return record.data as JsonRecord[];
  }

  return [];
};

const extractTotal = (payload: unknown, fallback: number) => {
  const record = asRecord(payload);
  if (record && typeof record.total === "number") {
    return record.total;
  }

  if (record && typeof record.total === "string") {
    const parsed = Number(record.total);
    if (Number.isFinite(parsed)) {
      return parsed;
    }
  }

  return fallback;
};

const readPagePayload = (
  payload: unknown,
): { data: JsonRecord[]; total: number } => {
  const data = extractCollection(payload);
  return { data, total: extractTotal(payload, data.length) };
};

const buildQueryString = (options: {
  pagination?: { page: number; perPage: number };
  sort?: { field: string; order?: "ASC" | "DESC" };
  filter?: Record<string, unknown>;
}) => {
  const searchParams = new URLSearchParams();

  if (options.pagination) {
    const { page, perPage } = options.pagination;
    if (page != null) {
      searchParams.set("page", String(page));
    }
    if (perPage != null) {
      searchParams.set("perPage", String(perPage));
    }
  }

  if (options.sort?.field) {
    searchParams.set("sort", options.sort.field);
    if (options.sort.order) {
      searchParams.set("order", options.sort.order);
    }
  }

  Object.entries(options.filter ?? {}).forEach(([key, value]) => {
    if (value === undefined || value === null || value === "") {
      return;
    }

    if (Array.isArray(value)) {
      value.forEach((entry) => {
        searchParams.append(key, String(entry));
      });
      return;
    }

    searchParams.set(key, String(value));
  });

  const query = searchParams.toString();
  return query.length ? `?${query}` : "";
};

const uploadImage = async (file?: File) => {
  if (!file) {
    return undefined;
  }

  const formData = new FormData();
  formData.append("file", file);
  const { json } = await httpClient(fileUploadUrl, {
    method: "POST",
    body: formData,
  });
  return json as { key: string; url?: string };
};

const extractImageFile = (data: MutablePayload) => {
  const candidate = data?.image;

  if (Array.isArray(candidate)) {
    const itemWithFile = candidate.find(hasRawFile);
    return itemWithFile?.rawFile;
  }

  if (hasRawFile(candidate)) {
    return candidate.rawFile;
  }

  return undefined;
};

const preparePayload = async (data: JsonRecord) => {
  const payload = { ...data } as MutablePayload;
  const rawFile = extractImageFile(payload);

  delete payload.image;
  delete payload.imageUrl;

  if (rawFile) {
    const result = await uploadImage(rawFile);
    if (result?.key) {
      payload.imageKey = result.key;
    }
  }

  return payload;
};

export const dataProvider: DataProvider = {
  getList: async (resource, params) => {
    const query = buildQueryString({
      pagination: params.pagination,
      sort: params.sort,
      filter: params.filter,
    });
    const { json } = await httpClient(`${getCollectionUrl(resource)}${query}`);
    return readPagePayload(json);
  },

  getOne: async (resource, params) => {
    const { json } = await httpClient(getRecordUrl(resource, params.id));
    return { data: json };
  },

  getMany: async (resource, params) => {
    const responses = await Promise.all(
      params.ids.map((id) => httpClient(getRecordUrl(resource, id))),
    );
    return { data: responses.map(({ json }) => json) };
  },

  getManyReference: async (resource, params) => {
    const filter = { ...(params.filter ?? {}), [params.target]: params.id };
    const query = buildQueryString({
      pagination: params.pagination,
      sort: params.sort,
      filter,
    });
    const { json } = await httpClient(`${getCollectionUrl(resource)}${query}`);
    return readPagePayload(json);
  },

  create: async (resource, params) => {
    const body = await preparePayload(params.data);
    const { json } = await httpClient(getCollectionUrl(resource), {
      method: "POST",
      body: JSON.stringify(body),
    });
    return { data: json };
  },

  update: async (resource, params) => {
    const body = await preparePayload(params.data);
    const { json } = await httpClient(getRecordUrl(resource, params.id), {
      method: "PUT",
      body: JSON.stringify(body),
    });
    return { data: json };
  },

  updateMany: async (resource, params) => {
    const responses = await Promise.all(
      params.ids.map((id) =>
        httpClient(getRecordUrl(resource, id), {
          method: "PUT",
          body: JSON.stringify(params.data),
        }),
      ),
    );
    return { data: responses.map(({ json }) => json.id) };
  },

  delete: async (resource, params) => {
    await httpClient(getRecordUrl(resource, params.id), { method: "DELETE" });
    return { data: params.previousData ?? { id: params.id } };
  },

  deleteMany: async (resource, params) => {
    await Promise.all(
      params.ids.map((id) =>
        httpClient(getRecordUrl(resource, id), { method: "DELETE" }),
      ),
    );
    return { data: params.ids };
  },
};

import {
  Box,
  Button,
  Card,
  CardActions,
  CardContent,
  CardHeader,
  CircularProgress,
  Stack,
  Typography,
} from "@mui/material";
import {
  Create,
  Edit,
  ImageField,
  ImageInput,
  List,
  NumberField,
  NumberInput,
  required,
  SelectField,
  SelectInput,
  Show,
  SimpleForm,
  SimpleShowLayout,
  TextField,
  TextInput,
  useListContext,
} from "react-admin";
import { Link } from "react-router-dom";

const stateChoices = [
  { id: "DRAFT", name: "Draft" },
  { id: "PUBLISHED", name: "Published" },
];

const BulletinForm = () => (
  <SimpleForm>
    <TextInput source="title" fullWidth validate={[required()]} />
    <TextInput
      source="description"
      multiline
      minRows={3}
      fullWidth
      validate={[required()]}
    />
    <SelectInput
      source="state"
      choices={stateChoices}
      validate={[required()]}
    />
    <TextInput source="contact" fullWidth validate={[required()]} />
    <NumberInput
      source="price"
      label="Price"
      min={0}
      step={0.01}
      validate={[required()]}
    />
    <ImageField source="imageUrl" label="Current image" />
    <ImageInput source="image" label="Upload image" accept="image/*">
      <ImageField source="src" title="title" />
    </ImageInput>
  </SimpleForm>
);

type BulletinRecord = {
  id: number;
  title: string;
  description: string;
  state: string;
  contact: string;
  price?: number;
  imageUrl?: string;
};

const formatPrice = (value?: number) =>
  typeof value === "number"
    ? new Intl.NumberFormat("en-US", {
        style: "currency",
        currency: "USD",
      }).format(value)
    : "—";

const BulletinGrid = () => {
  const { data, isLoading } = useListContext<BulletinRecord>();

  if (isLoading) {
    return (
      <Stack alignItems="center" mt={4}>
        <CircularProgress />
      </Stack>
    );
  }

  if (!data?.length) {
    return (
      <Typography variant="body1" sx={{ p: 2 }}>
        No bulletins to display.
      </Typography>
    );
  }

  return (
    <Box
      sx={{
        p: 2,
        display: "flex",
        flexWrap: "wrap",
        gap: 2,
        justifyContent: "center",
      }}
    >
      {data.map((record) => (
        <Card
          key={record.id}
          sx={{
            width: 360,
            maxWidth: "100%",
            display: "flex",
            flexDirection: "column",
          }}
        >
          {record.imageUrl && (
            <Box
              component={Link}
              to={`/bulletins/${record.id}/show`}
              sx={{ display: "block", lineHeight: 0 }}
            >
              <Box
                component="img"
                src={record.imageUrl}
                alt={record.title}
                sx={{
                  height: 180,
                  width: "100%",
                  objectFit: "cover",
                  display: "block",
                }}
              />
            </Box>
          )}
          <CardHeader
            title={record.title}
            subheader={formatPrice(record.price)}
          />
          <CardContent sx={{ flexGrow: 1 }}>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
              {record.description?.slice(0, 140) ?? ""}{" "}
              {record.description && record.description.length > 140 ? "…" : ""}
            </Typography>
            <Typography variant="caption" color="text.secondary">
              State: {record.state}
            </Typography>
            <Typography
              variant="caption"
              color="text.secondary"
              display="block"
            >
              Contact: {record.contact}
            </Typography>
          </CardContent>
          <CardActions>
            <Button
              component={Link}
              to={`/bulletins/${record.id}/show`}
              size="small"
            >
              View
            </Button>
            <Button
              component={Link}
              to={`/bulletins/${record.id}`}
              size="small"
            >
              Edit
            </Button>
          </CardActions>
        </Card>
      ))}
    </Box>
  );
};

export const BulletinList = () => (
  <List perPage={9}>
    <BulletinGrid />
  </List>
);

export const BulletinShow = () => (
  <Show>
    <SimpleShowLayout>
      <TextField source="id" />
      <TextField source="title" />
      <TextField source="description" />
      <SelectField source="state" choices={stateChoices} />
      <TextField source="contact" />
      <NumberField
        source="price"
        label="Price"
        options={{ style: "currency", currency: "USD" }}
      />
      <ImageField source="imageUrl" label="Image" />
    </SimpleShowLayout>
  </Show>
);

export const BulletinEdit = () => (
  <Edit>
    <BulletinForm />
  </Edit>
);

export const BulletinCreate = () => (
  <Create>
    <BulletinForm />
  </Create>
);

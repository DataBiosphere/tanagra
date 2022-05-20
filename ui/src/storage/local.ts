import * as tanagra from "tanagra-api";
import { registerStoragePlugin, StoragePlugin } from "./storage";

const storageKey = "tanagra-userdata";

@registerStoragePlugin()
// eslint-disable-next-line @typescript-eslint/no-unused-vars
class LocalStoragePlugin implements StoragePlugin {
  async store(data: tanagra.UserData): Promise<void> {
    localStorage.setItem(
      storageKey,
      JSON.stringify(tanagra.UserDataToJSON(data))
    );
  }

  async load(): Promise<tanagra.UserData | undefined> {
    const text = localStorage.getItem(storageKey);
    if (!text) {
      return undefined;
    }
    return tanagra.UserDataFromJSON(JSON.parse(text));
  }
}

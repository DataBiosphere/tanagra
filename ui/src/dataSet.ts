export class DataSet {
  constructor() {
    this.included = new Array<Group>();
  }

  included: Array<Group>;
}

export class Group {
  constructor() {
    this.id = "";
    this.criteria = new Array<Criteria>();
  }

  id: string;
  criteria: Array<Criteria>;
}

export class Criteria {
  constructor() {
    this.id = "";
    this.code = "";
    this.name = "";
  }

  id: string;
  code: string;
  name: string;
}

class BluetoothDevice {
  String? name;
  String? address;
  int? type;
  bool? connected;

  BluetoothDevice({this.name, this.address, this.type, this.connected});

  BluetoothDevice.fromJson(json) {
    name = json['name'];
    address = json['address'];
    type = json['type'];
    connected = json['connected'];
  }

  Map<String, dynamic> toJson() {
    final Map<String, dynamic> data = {};
    data['name'] = name;
    data['address'] = address;
    data['type'] = type;
    data['connected'] = connected;
    return data;
  }
}

export type AWSRegion = 
  | 'us-east-1' | 'us-east-2' | 'us-west-1' | 'us-west-2'
  | 'eu-west-1' | 'eu-west-2' | 'eu-west-3' | 'eu-central-1' | 'eu-north-1'
  | 'ap-northeast-1' | 'ap-northeast-2' | 'ap-northeast-3'
  | 'ap-southeast-1' | 'ap-southeast-2' | 'ap-southeast-3'
  | 'ap-south-1' | 'ap-east-1'
  | 'ca-central-1'
  | 'sa-east-1'
  | 'af-south-1'
  | 'me-south-1';

const regionToAirportCode: { [key in AWSRegion]: string } = {
  'us-east-1': 'IAD',
  'us-east-2': 'CMH',
  'us-west-1': 'SFO',
  'us-west-2': 'PDX',
  'eu-west-1': 'DUB',
  'eu-west-2': 'LHR',
  'eu-west-3': 'CDG',
  'eu-central-1': 'FRA',
  'eu-north-1': 'ARN',
  'ap-northeast-1': 'NRT',
  'ap-northeast-2': 'ICN',
  'ap-northeast-3': 'KIX',
  'ap-southeast-1': 'SIN',
  'ap-southeast-2': 'SYD',
  'ap-southeast-3': 'JKT',
  'ap-south-1': 'BOM',
  'ap-east-1': 'HKG',
  'ca-central-1': 'YUL',
  'sa-east-1': 'GRU',
  'af-south-1': 'CPT',
  'me-south-1': 'BAH'
};

export const AWSRegionUtils = {
  getAirportCode: (region: AWSRegion): string => {
    const airportCode = regionToAirportCode[region];
    if (!airportCode) {
      throw new Error(`No airport code found for region: ${region}`);
    }
    return airportCode;
  }
};







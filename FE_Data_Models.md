# Frontend Data Models (TypeScript Interfaces)

This file defines the TypeScript interfaces corresponding to the backend data transfer objects (DTOs) and database entities.

## Type Mappings Reference

When developing on the frontend, map Java backend datatypes to TypeScript types using this reference table:

| Java Type | TypeScript Type | Format / Example |
| :--- | :--- | :--- |
| `Integer` / `Long` | `number` | `102` |
| `Double` / `BigDecimal` / `Float` | `number` | `150.50` |
| `String` / `char` | `string` | `"SUBMITTED"` |
| `Boolean` / `boolean` | `boolean` | `true` |
| `LocalDate` | `string` | `"YYYY-MM-DD"` (e.g., `"2026-12-31"`) |
| `LocalDateTime` | `string` | `"YYYY-MM-DDTHH:mm:ss"` (e.g., `"2026-05-22T04:30:00"`) |
| `List<T>` / `Set<T>` | `T[]` | Array of objects |
| `Map<K, V>` | `Record<string, V>` | Key-value dictionary |

---

## 1. Database Entities (3NF Persistence Layer)

These interfaces represent the normalized relational tables (21 tables) stored in the database. In nested objects, their parent-child relationships are preserved.

### AccountEntity
```typescript
export interface AccountEntity {
  accountId?: number; // Primary Key (nullable when creating)
  citizenId: number; // Foreign Key pointing to CitizenLocalEntity
  roleId: number; // Foreign Key pointing to RoleEntity
  accountStatus?: string; // e.g., "ACTIVE", "INACTIVE"
  statusNote?: string; // Reason for lock/activation
}
```

### AreaEntity
```typescript
export interface AreaEntity {
  areaId?: number; // Primary Key
  districtCode: string; // District code from standard VNeID/Admin division
  wardCode: string; // Ward code
  streetName?: string; // Street name (optional)
  positionLevel: number; // Street position grouping level (1, 2, 3, 4)
  landQuota?: number; // Quota for residential land (in sq. meters)
}
```

### CitizenLocalEntity
```typescript
export interface CitizenLocalEntity {
  citizenId?: number; // Primary Key
  cccdNumber: string; // Citizen identity card number (Unique)
  fullName: string; // Full name matching VNeID registry
  email?: string;
  phoneNumber?: string;
}
```

### ComplaintEntity
```typescript
export interface ComplaintEntity {
  id?: number; // Primary Key
  citizen: CitizenLocalEntity; // Eagerly loaded citizen who appealed
  record?: RecordEntity; // Optional associated tax record
  content: string; // Complaint feedback text
  status?: "PENDING" | "RESOLVED";
  responseNote?: string; // Official response text
  createdAt: string; // ISO 8601 Timestamp
  updatedAt: string; // ISO 8601 Timestamp
}
```

### LandOwnerEntity
```typescript
export interface LandOwnerEntity {
  ownershipId?: number; // Primary Key
  citizenId: number; // Foreign Key pointing to Citizen
  landParcelId: number; // Foreign Key pointing to LandParcel
  ownershipType?: string; // e.g., "SO_HUU_RIENG", "SO_HUU_CHUNG"
}
```

### LandParcelEntity
```typescript
export interface LandParcelEntity {
  landParcelId?: number; // Primary Key
  landTypeId: number; // Foreign Key referencing LandTypeEntity
  areaId: number; // Foreign Key referencing AreaEntity
  parcelNumber: string; // Map coordinate parcel number
  mapSheetNumber: string; // Map coordinate sheet number
  areaSize: number; // Land size in sq. meters (BigDecimal mapped to number)
  usageDuration?: string; // e.g., "Lâu dài", "Đến năm 2050"
  usageType?: string; // Purpose description
  usageOrigin?: string; // Land allocation origin details
  address: string; // Physical address of parcel
  certificateNumber?: string; // Red Book Certificate Number
  gcnBookNumber?: string; // Entry book registration number
  notes?: string;
}
```

### LandPriceEntity
```typescript
export interface LandPriceEntity {
  priceId?: number; // Primary Key
  landTypeId: number; // Land category ID
  areaId: number; // Location ID
  unitPrice: number; // Cost per sq. meter
  appliedFrom: string; // Effective Date ("YYYY-MM-DD")
}
```

### LandTypeEntity
```typescript
export interface LandTypeEntity {
  landTypeId?: number; // Primary Key
  typeCode: string; // e.g., "ONT", "LUC", "CLN"
  typeName: string; // e.g., "Đất ở nông thôn"
  isTaxPayment?: boolean; // Determines if subject to recurring land tax
}
```

### NotificationEntity
```typescript
export interface NotificationEntity {
  notiId?: number; // Primary Key
  accountId: number; // Target account ID
  title: string; // Notification title
  content: string; // Body detail
  notiType: string; // e.g., "COMPLAINT_RESOLVED", "DECLARATION_APPROVED"
  isRead?: boolean; // Read flag
  createdAt?: string; // ISO 8601 Timestamp
}
```

### ProcessingLogEntity
```typescript
export interface ProcessingLogEntity {
  plogId?: number; // Primary Key
  recordId: number; // Target dossier ID
  processorAccountId: number; // Account who processed step
  processingStep: string; // Action description
  oldStatus?: string;
  newStatus: string;
  processorNotes?: string;
  processedAt?: string; // ISO 8601 Timestamp
}
```

### ReconciliationBatchEntity
```typescript
export interface ReconciliationBatchEntity {
  batchId?: number;
  officerAccountId: number;
  totalRecords?: number;
  matchedCount?: number;
  errorCount?: number;
  batchNotes?: string;
  createdAt?: string;
}
```

### ReconciliationLogEntity
```typescript
export interface ReconciliationLogEntity {
  logId?: number;
  transactionCode: string;
  amountReceived: number;
  bankTransId?: string;
  webhookPayload?: string; // Stored JSON payload from PayOS/Bank webhook
  status?: "MATCHED" | "DISCREPANCY" | "UNRESOLVED";
  createdAt?: string;
}
```

### RecordDocumentEntity
```typescript
export interface RecordDocumentEntity {
  documentId?: number;
  fileName: string; // Saved name on physical disk
  fileUrl: string; // Accessible download route URL
  fileType?: string; // MIME type
}
```

### RecordEntity
```typescript
export interface RecordEntity {
  recordId?: number; // Primary Key
  citizenId: number; // Submitting citizen
  landParcelId: number; // Land parcel reference
  recordCategory: "TAX_DECLARATION" | "MUTATION" | "OTHER";
  currentStatus?: "SUBMITTED" | "FRAUD_SUSPECTED" | "VERIFIED" | "APPROVED" | "REJECTED" | "COMPLETED";
  submittedAt?: string; // ISO 8601 Timestamp
  taxDeclaration?: TaxDeclarationEntity; // Nested child (1-to-1 relationship)
}
```

### RefreshTokenEntity
```typescript
export interface RefreshTokenEntity {
  tokenId?: number;
  token: string;
  accountId: number;
  expiresAt: string;
  isRevoked: boolean;
  createdAt?: string;
}
```

### RoleEntity
```typescript
export interface RoleEntity {
  roleId?: number;
  roleCode: "ROLE_ADMIN" | "ROLE_TAX_OFFICER" | "ROLE_LAND_OFFICER" | "ROLE_CITIZEN";
  roleName: string;
}
```

### TaxDeclarationEntity
```typescript
export interface TaxDeclarationEntity {
  declarationId?: number;
  recordId?: number; // Back-reference to parent RecordEntity ID
  record?: RecordEntity;
  declaredArea?: number; // Declared area size
  declaredUsage?: string; // Declared category description
  declarationNotes?: string;
  createdAt?: string;
}
```

### TaxExemptSubjectEntity
```typescript
export interface TaxExemptSubjectEntity {
  exemptId?: number;
  citizenId: number;
  uploadedByAccount?: number;
  fullName?: string;
  exemptionReason?: string;
  discountRate?: number; // Percentage discount (BigDecimal e.g., 0.50 for 50%)
  appliedYear?: number;
  uploadedAt?: string;
}
```

### TaxPaymentEntity
```typescript
export interface TaxPaymentEntity {
  payId?: number;
  recordId?: number; // Reference to approved record
  landParcelId: number;
  taxYear: number;
  totalAmountDue: number; // Computed final tax due
  dueDate: string; // Payment deadline ("YYYY-MM-DD")
  paymentStatus?: "UNPAID" | "AWAITING_PAYMENT" | "PAID" | "CANCELLED" | "WAIVED" | "DISCREPANCY";
  transactionCode?: string; // PayOS order Code or bank ref
  paidAt?: string; // ISO 8601 Timestamp
}
```

### TaxRateEntity
```typescript
export interface TaxRateEntity {
  rateId?: number;
  taxName: string; // e.g., "Thuế đất ở định mức"
  rateValue: number; // Coefficient (e.g., 0.0003 for 0.03%)
  rateCode: string; // e.g., "DINH_MUC"
}
```

---

## 2. Request & Response DTOs

These interfaces represent payloads exchanged with REST endpoints.

### ApiResponse<T>
```typescript
export interface ApiResponse<T> {
  success?: boolean;
  message?: string;
  data?: T;
  errorCode?: string;
}
```

### AuthRequest / AuthResponse
```typescript
export interface QrLoginRequest {
  qrToken: string;
}

export interface SwitchRoleRequest {
  targetRole: "ROLE_ADMIN" | "ROLE_TAX_OFFICER" | "ROLE_LAND_OFFICER" | "ROLE_CITIZEN";
}

export interface AuthResponse {
  token: string; // JWT access token
  refreshToken: string;
  userId: number;
  fullName: string;
  cccdNumber: string;
  activeRole: "ROLE_ADMIN" | "ROLE_TAX_OFFICER" | "ROLE_LAND_OFFICER" | "ROLE_CITIZEN";
  roles: string[];
}
```

### CalculateTaxRequest / CalculateTaxResponse
```typescript
export interface CalculateTaxRequest {
  areaId: number;
  landTypeId: number;
  declaredArea: number;
}

export interface CalculateTaxResponse {
  unitPrice: number;
  taxRate: number;
  calculatedAmount: number;
}
```

### CitizenIdentityDTO
```typescript
export interface CitizenIdentityDTO {
  cccdNumber: string;
  fullName: string;
  dob?: string; // Date ("YYYY-MM-DD")
  gender?: string;
  email?: string;
  phoneNumber?: string;
}
```

### CreateUserRequest
```typescript
export interface CreateUserRequest {
  cccdNumber: string;
  fullName: string;
  phoneNumber?: string;
  roleId: number;
}
```

### DossierStatusReportDTO
```typescript
export interface DossierStatusReportDTO {
  status: string;
  count: number;
  percentage: number;
}
```

### ForwardRecordRequest
```typescript
export interface ForwardRecordRequest {
  forwardNote: string;
}
```

### LandParcelDTO
```typescript
export interface LandParcelDTO {
  landParcelId: number;
  landTypeId: number;
  areaId: number;
  parcelNumber: string;
  mapSheetNumber: string;
  areaSize: number;
  usageDuration?: string;
  usageType?: string;
  usageOrigin?: string;
  address: string;
  certificateNumber?: string;
  gcnBookNumber?: string;
  attachedHouse?: string;
  attachedOther?: string;
  landInfoPdf?: string;
  notes?: string;
  ownerCccd?: string;
}
```

### MutationResponseDTO
```typescript
export interface MutationResponseDTO {
  id: number;
  parcelId: number;
  requesterCccd: string;
  mutationType: string;
  status: string;
  note?: string;
  createdAt: string;
}
```

### PaymentRequest
```typescript
export interface PaymentRequest {
  taxRecordId: number;
  citizenId: number;
  amount: number;
  paymentMethod?: string;
  returnUrl?: string; // Return URL for PayOS checkout redirection
  cancelUrl?: string; // Cancel URL for PayOS checkout redirection
}
```

### RecordRequestDTO
```typescript
export interface TaxDeclarationSubDTO {
  declaredArea?: number;
  declaredPurpose?: string;
  phoneNumber?: string;
  address?: string;
}

export interface RecordRequestDTO {
  citizenId?: number;
  landParcelId?: number;
  recordCategory: "TAX_DECLARATION" | "MUTATION" | "OTHER";
  taxDeclaration?: TaxDeclarationSubDTO;
}
```

### RevenueReportDTO
```typescript
export interface RevenueReportDTO {
  month: number;
  totalRevenue: number;
}
```

### ReviewDeclarationRequest
```typescript
export interface ReviewDeclarationRequest {
  processorNotes?: string;
}
```

### RoleDTO
```typescript
export interface RoleDTO {
  id: number;
  roleCode: string;
  roleName: string;
}
```

### TaxDeclarationDTO
```typescript
export interface TaxDeclarationDTO {
  id?: number;
  landParcelId: number;
  citizenId: number;
  taxYear: number;
  taxableArea: number;
  taxRate?: number;
  taxAmount?: number;
  status?: string;
  notes?: string;
}
```

### TaxDeclarationRequest
```typescript
export interface TaxDeclarationRequest {
  parcelId: number;
  declaredArea: number;
  attachmentIds?: number[];
}
```

### TaxDeclarationResponse
```typescript
export interface TaxDeclarationResponse {
  recordId: number;
  citizenId: number;
  parcelId: number;
  declaredArea: number;
  declaredUsage?: string;
  status: "SUBMITTED" | "FRAUD_SUSPECTED" | "VERIFIED" | "APPROVED" | "REJECTED" | "COMPLETED";
  declarationNotes?: string;
  createdAt: string;
}
```

### UpdateRoleRequest
```typescript
export interface UpdateRoleRequest {
  roleName?: string;
  roleCode?: string;
}
```

### UserAdminDTO
```typescript
export interface UserAdminDTO {
  cccdNumber: string;
  fullName: string;
  role: string;
  status: string;
  phoneNumber?: string;
  email?: string;
}
```

### VneidAuthResponse
```typescript
export interface VneidAuthResponse {
  token: string;
  tokenType: string;
  userId: string;
  cccdNumber: string;
  fullName: string;
  email?: string;
  role: string;
  message?: string;
}
```

### VneidQrStatusResponse
```typescript
export interface VneidQrStatusResponse {
  status: "PENDING" | "SCANNED" | "EXPIRED" | "AUTHENTICATED";
  token?: string;
}
```
